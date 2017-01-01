import groovy.json.JsonSlurper

metadata {
    definition(
        name: "Garage Door",
        namespace: "rrudduck",
        author: "Robert Rudduck",
        description: "Controls garage doors through an api."
    ) {
        capability "Actuator"
        capability "Configuration"
        capability "Contact Sensor"
        capability "Door Control"
        capability "Garage Door Control"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
    }

    simulator {

    }

    tiles {
        standardTile("toggle", "device.door", width: 3, height: 2) {
			state "closed", label:'${name}', action:"open", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821"
			state "open", label:'${name}', action:"close", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e"
        }

        standardTile("refresh", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh", icon:"st.secondary.refresh"
		}
        
        standardTile("open", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"open", icon:"st.doors.garage.garage-opening"
		}
        
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"close", icon:"st.doors.garage.garage-closing"
		}

        main "toggle"

        details(["toggle", "refresh", "open", "close"])
    }

    preferences {
        input name: "ip", type: "text", title: "IP Address", required: true, multiple: false
        input name: "port", type: "number", title: "Port", required: true, multiple: false
        input name: "doorId", type: "number", title: "Door Id", required: true, multiple: false
    }
}

def configure() {
	
}

def parse(String description) {   
    if (description == 'updated') {
    	return
  	}
    
  	def descMap = parseDescriptionAsMap(description)
    
  	if (descMap.containsKey("body")) {
    	def body = parseBase64Json(descMap["body"])
        
    	if (body.containsKey("result")) {
            def result = body.result
            def resultStr = result == 0 ? "open" : "closed"
      		sendEvent(name: "door", value: resultStr)
            sendEvent(name: "contact", value: resultStr)
    	}
  }
}

def open() {
    request("/v1/doors/open/${doorId}", "POST")
}

def close() {
    request("/v1/doors/close/${doorId}", "POST")
}

def poll() {
    pollInternal(false)
}

def refresh() {
    pollInternal(true)
}

private pollInternal(boolean isRefresh) {
	setDeviceNetworkId()
    request("/v1/doors/${doorId}", "GET")
}

private setDeviceNetworkId() {
    if (ip == null || port == null ) return
    def ip = convertIPtoHex(ip)
    def port = convertPortToHex(port)
    def newId = "${ip}:${port}"
    if (device.deviceNetworkId != newId) {
  		device.deviceNetworkId = "${ip}:${port}"
        log.debug "Device Network Id set to ${device.deviceNetworkId}"
    }
}

def request(path, method, body = "") {
	def hubAction = new physicalgraph.device.HubAction(
		method: method,
      	path: path,
      	body: body,
      	headers: [ HOST: getHostAddress() ]
    )
    
    return hubAction
}

def parseBase64Json(String input) {
 	def sluper = new JsonSlurper();
  	sluper.parseText(new String(input.decodeBase64()))
}

def parseDescriptionAsMap(String description) {
 	description.split(',').inject([:]) { map, param ->
    	def nameAndValue = param.split(":")
    	map += [(nameAndValue[0].trim()) : (nameAndValue[1].trim())]
  	}
}

private String getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ip) {
    String hex = ip.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}