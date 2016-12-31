metadata {
    definition(
        name: "Garage Door",
        namespace: "rrudduck",
        author: "Robert Rudduck",
        description: "Controls garage doors through an api."
    ) {
        capability "Actuator"
        capability "Door Control"
        capability "Garage Door Control"
        capability "Contact Sensor"
        capability "Refresh"
        capability "Sensor"

        attribute "status"

        command "toggle"
    }

    simulator {

    }

    tiles {
        standardTile("toggle", "device.door", width: 2, height: 2) {
			state("closed", label:'${name}', action:"changeState", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"opening")
			state("open", label:'${name}', action:"changeState", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-closed", backgroundColor:"#ffe71e")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#ffe71e")
        }

        standardTile("refresh", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh", icon:"st.secondary.refresh"
		}

        main "toggle"

        details(["toggle", "refresh"])
    }

    preferences {
        input name: "ip", type: "text", title: "IP Address", required: true, multiple: false
        input name: "port", type: "number", title: "Port", required: true, multiple: false
    }
}

def parse(String description) {
    log.trace "parse($description)"
}

def toggle() {

}

def poll() {

}

def refresh() {
    poll()
}

private String NetworkDeviceId(){
    def ip = convertIPtoHex(settings.ip).toUpperCase()
    def port = convertPortToHex(settings.port)
    return "$ip:$port"
}

private String convertIPtoHex(ip) {
    String hex = ip.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}