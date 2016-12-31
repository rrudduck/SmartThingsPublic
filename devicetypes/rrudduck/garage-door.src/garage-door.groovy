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
        capability "Polling"

        attribute "status", "string"

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
	log.debug "Event: poll"
    pollInternal(false)
}

def refresh() {
	log.debug "Event: refresh"
    pollInternal(true)
}

private pollInternal(boolean isRefresh) {
	setDeviceNetworkId()
}

private setDeviceNetworkId(){
	log.debug "Setting device network id dynamically."
    if (ip == null || port == null ) return
    def ip = convertIPtoHex(ip)
    def port = convertPortToHex(port)
  	device.deviceNetworkId = "$ip$port"
  	log.debug "Device Network Id set to ${ip}${port}"
}

private String convertIPtoHex(ip) {
    String hex = ip.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}