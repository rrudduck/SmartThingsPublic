import groovy.json.*

metadata {
    definition(
        name: "RGB Light",
        namespace: "rrudduck",
        author: "Robert Rudduck",
        description: "Controls an RGB light."
    ) {
        capability "Actuator"
        capability "Color Control"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"

        command "reset"
    }

    simulator {

    }

    tiles {
        standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
            state "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
            state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
        }

        standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat") {
            state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        controlTile("rgbSelector", "device.color", "color", height: 3, width: 3, inactiveLabel: false) {
            state "color", action:"setColor"
        }
    }

    preferences {
        input name: "ip", type: "text", title: "IP Address", required: true, multiple: false
        input name: "port", type: "number", title: "Port", required: true, multiple: false
    }
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
      		sendEvent(name: "color", value: result)
    	}
  	}
}

def refresh() {
    setDeviceNetworkId()
    request("/v1/color", "GET")
}

def reset() {
	sendEvent(name: "switch", value: "off")
	setColor(hex: "#000000")
}

def on() {
    sendEvent(name: "switch", value: "on")
    setColor(hex: "#ffffff")
}

def off() { 
	sendEvent(name: "switch", value: "off")
    reset()
}

def setSaturation(percent) {
	log.debug "setSaturation($percent)"
	setColor(saturation: percent)
}

def setHue(value) {
	log.debug "setHue($value)"
	setColor(hue: value)
}

def setColor(value) {
	def result = []
	log.debug "setColor: $value"
    def json = new JsonBuilder()
	if (value.hex) {
        json color: value.hex
	} else {
		def hue = value.hue ?: device.currentValue("hue")
		def saturation = value.saturation ?: device.currentValue("saturation")
		if(hue == null) hue = 13
		if(saturation == null) saturation = 13
		def rgb = huesatToRGB(hue, saturation)
        json color: "${rgb[0]},${rgb[1]},${rgb[2]}"
	}

	if(value.hue) sendEvent(name: "hue", value: value.hue)
	if(value.hex) sendEvent(name: "color", value: value.hex)
	if(value.switch) sendEvent(name: "switch", value: value.switch)
	if(value.saturation) sendEvent(name: "saturation", value: value.saturation)

	request("/v1/color", "POST", json.toString())
}

def setDeviceNetworkId() {
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
      	headers: [ "Host": getHostAddress(), "Content-Type": "application/json" ]
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

def getHostAddress() {
	return "${ip}:${port}"
}

def convertIPtoHex(ip) {
    String hex = ip.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

def convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

def rgbToHSV(red, green, blue) {
	float r = red / 255f
	float g = green / 255f
	float b = blue / 255f
	float max = [r, g, b].max()
	float delta = max - [r, g, b].min()
	def hue = 13
	def saturation = 0
	if (max && delta) {
		saturation = 100 * delta / max
		if (r == max) {
			hue = ((g - b) / delta) * 100 / 6
		} else if (g == max) {
			hue = (2 + (b - r) / delta) * 100 / 6
		} else {
			hue = (4 + (r - g) / delta) * 100 / 6
		}
	}
	[hue: hue, saturation: saturation, value: max * 100]
}

def huesatToRGB(float hue, float sat) {
	while(hue >= 100) hue -= 100
	int h = (int)(hue / 100 * 6)
	float f = hue / 100 * 6 - h
	int p = Math.round(255 * (1 - (sat / 100)))
	int q = Math.round(255 * (1 - (sat / 100) * f))
	int t = Math.round(255 * (1 - (sat / 100) * (1 - f)))
	switch (h) {
		case 0: return [255, t, p]
		case 1: return [q, 255, p]
		case 2: return [p, 255, t]
		case 3: return [p, q, 255]
		case 4: return [t, p, 255]
		case 5: return [255, p, q]
	}
}