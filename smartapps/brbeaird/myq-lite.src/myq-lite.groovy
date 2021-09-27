/**
 * -----------------------
 * ------ SMART APP ------
 * -----------------------
 *
 *  MyQ Lite
 *
 *  Copyright 2019 Jason Mok/Brian Beaird/Barry Burke/RBoy Apps
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

 import java.security.MessageDigest
 import groovy.transform.Field

 @Field CLIENT_SECRET = "UD4DXnKyPWq25BSw"
 @Field CLIENT_ID = "IOS_CGI_MYQ"
 @Field AUTH_HOSTNAME = "https://partner-identity.myq-cloud.com"
 @Field COMMAND_HOSTNAME = "https://account-devices-gdo.myq-cloud.com"
 @Field REDIRECT_URI = "com.myqops://ios"

String appVersion() { return "3.1.3" }
String appModified() { return "2020-07-03"}
String appAuthor() { return "Brian Beaird" }
String gitBranch() { return "brbeaird" }
String getAppImg(imgName) 	{ return "https://raw.githubusercontent.com/${gitBranch()}/SmartThings_MyQ/master/icons/$imgName" }

definition(
	name: "MyQ Lite",
	namespace: "brbeaird",
	author: "Jason Mok/Brian Beaird/Barry Burke",
	description: "Integrate MyQ with Smartthings",
	category: "SmartThings Labs",
	importUrl: "https://raw.githubusercontent.com/dcmeglio/hubitat-myq/master/smartapps/brbeaird/myq-lite.src/myq-lite.groovy",
	iconUrl:   "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq.png",
	iconX2Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/brbeaird/SmartThings_MyQ/master/icons/myq@3x.png"
)

preferences {
	page(name: "mainPage", title: "MyQ Lite")
    page(name: "prefLogIn", title: "MyQ")
    page(name: "loginResultPage", title: "MyQ")
	page(name: "prefListDevices", title: "MyQ")
    page(name: "sensorPage", title: "MyQ")
    page(name: "noDoorsSelected", title: "MyQ")
    page(name: "summary", title: "MyQ")
    page(name: "prefUninstall", title: "MyQ")
}

def appInfoSect(sect=true)	{
	def str = ""
	str += "${app?.name} (v${appVersion()})"
	str += "\nAuthor: ${appAuthor()}"
	section() { paragraph str, image: getAppImg("myq@2x.png") }
}

def mainPage() {

    if (state.previousVersion == null){
        state.previousVersion = 0;
    }

    //Brand new install (need to grab version info)
    if (!state.latestVersion){
        state.currentVersion = [:]
        state.currentVersion['SmartApp'] = appVersion()
    }
    //Version updated
    else{
        state.previousVersion = appVersion()
    }

    //If fresh install, go straight to login page
    if (!settings.username){
    	state.lastPage = "prefListDevices"
        return prefLogIn()
    }

    state.lastPage = "mainPage"

    dynamicPage(name: "mainPage", nextPage: "", uninstall: false, install: true) {
        appInfoSect()
        def devs = refreshChildren()
        section("MyQ Account"){
            paragraph title: "", "Email: ${settings.username}"
            href "prefLogIn", title: "", description: "Tap to modify account", params: [nextPageName: "mainPage"]
        }
        section("Connected Devices") {
        	paragraph title: "", "${devs?.size() ? devs?.join("\n") : "No MyQ Devices Connected"}"
            href "prefListDevices", title: "", description: "Tap to modify devices"
        }
        section("Uninstall") {
            paragraph "Tap below to completely uninstall this SmartApp and devices (doors and lamp control devices will be force-removed from automations and SmartApps)"
            href(name: "", title: "",  description: "Tap to Uninstall", required: false, page: "prefUninstall")
        }
		section("Logging") {
			paragraph "Enable debug logs? Will be disabled automatically in 30 minutes"
        	input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
		}
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def versionCompare(deviceName){
    if (!state.currentVersion || !state.latestVersion || state.latestVersion == [:]){
        return 'latest'
    }
    if (state.currentVersion[deviceName] == state.latestVersion[deviceName]){
    	return 'latest'
    }
    else{
   		return "${state.latestVersion[deviceName]} available"
    }
}

def refreshChildren(){
	state.currentVersion = [:]
    state.currentVersion['SmartApp'] = appVersion()
    def devices = []
    childDevices.each { child ->
    	def myQId = child.getMyQDeviceId() ? "ID: ${child.getMyQDeviceId()}" : 'Missing MyQ ID'
        def devName = child.name
        if (child.typeName == "MyQ Garage Door Opener"){
        	devName = devName + " (${child.currentContact})  ${myQId}"
            state.currentVersion['DoorDevice'] = child.showVersion()
        }
        else if (child.typeName == "MyQ Garage Door Opener-NoSensor"){
        	devName = devName + " (No sensor)   ${myQId}"
            state.currentVersion['DoorDeviceNoSensor'] = child.showVersion()
		}
        else if (child.typeName == "MyQ Light Controller"){
        	devName = devName + " (${child.currentSwitch})  ${myQId}"
            state.currentVersion['LightDevice'] = child.showVersion()
        }
        else{
        	return	//Ignore push-button devices
		}
        devices.push(devName)
    }
    return devices
}

/* Preferences */
def prefLogIn(params) {
    state.installMsg = ""
    def showUninstall = username != null && password != null
	return dynamicPage(name: "prefLogIn", title: "Connect to MyQ", nextPage:"loginResultPage", uninstall:false, install: false, submitOnChange: true) {
		section("Login Credentials"){
			input("username", "email", title: "Username", description: "MyQ Username (email address)")
			input("password", "password", title: "Password", description: "MyQ password")
		}
	}
}

def loginResultPage(){
	if (logEnable) log.debug "login result next page: ${state.lastPage}"
    if (forceLogin()) {
    	if (state.lastPage == "prefListDevices")
        	return prefListDevices()
        else
        	return mainPage()
    }
    else{
    	return dynamicPage(name: "loginResultPage", title: "Login Error", install:false, uninstall:false) {
			section(""){
				paragraph "The username or password you entered is incorrect. Go back and try again. "
			}
		}
    }
}

def prefUninstall() {
    if (logEnable) log.debug "Removing MyQ Devices..."
    def msg = ""
    childDevices.each {
		try{
			deleteChildDevice(it.deviceNetworkId, true)
            msg = "Devices have been removed. Tap remove to complete the process."

		}
		catch (e) {
			if (logEnable) log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
            msg = "There was a problem removing your device(s). Check the IDE logs for details."
		}
	}

    return dynamicPage(name: "prefUninstall",  title: "Uninstall", install:false, uninstall:true) {
        section("Uninstallation"){
			paragraph msg
		}
    }
}

def getDeviceSelectionList(deviceType){
	def testing
}

def prefListDevices() {
    state.lastPage = "prefListDevices"
    if (login()) {
    	getMyQDevices()

        state.doorList = [:]
        state.lightList = [:]
        state.MyQDataPending.each { id, device ->
        	if (device.typeName == 'door'){
            	state.doorList[id] = device.name
            }
            else if (device.typeName == 'light'){
            	state.lightList[id] = device.name
            }
        }

		if ((state.doorList) || (state.lightList)){
        	def nextPage = "sensorPage"
            if (!state.doorList){nextPage = "summary"}  //Skip to summary if there are no doors to handle
                return dynamicPage(name: "prefListDevices",  title: "Devices", nextPage:nextPage, install:false, uninstall:false) {
                    if (state.doorList) {
                        section("Select which garage door/gate to use"){
                            input(name: "doors", type: "enum", required:false, multiple:true, options:state.doorList)
                        }
                    }
                    if (state.lightList) {
                        section("Select which lights to use"){
                            input(name: "lights", type: "enum", required:false, multiple:true, options:state.lightList)
                        }
                    }
                    section("Advanced (optional)", hideable: true, hidden:true){
        	            paragraph "BETA: Enable the below option if you would like to force the Garage Doors to behave as Door Locks (sensor required)." +
                        			"This may be desirable if you only want doors to open up via PIN with Alexa voice commands. " +
                                    "Note this is still considered highly experimental and may break many other automations/apps that need the garage door capability."
        	            input "prefUseLockType", "bool", required: false, title: "Create garage doors as door locks?"
					}
                }

        }else {
			return dynamicPage(name: "prefListDevices",  title: "Error!", install:false, uninstall:true) {
				section(""){
					paragraph "Could not find any supported device(s). Please report to author about these devices: " +  state.unsupportedList
				}
			}
		}
	} else {
		return prefLogIn([nextPageName: "prefListDevices"])
	}
}


def sensorPage() {

    //If MyQ ID changes, the old stale ID will still be listed in the settings array. Let's get a clean count of valid doors selected
    state.validatedDoors = []
    if (doors instanceof List && doors.size() > 1){
        doors.each {
            if (state.MyQDataPending[it] != null){
                state.validatedDoors.add(it)
            }
        }
    }
    else{
    	state.validatedDoors = doors	//Handle single door
    }

    return dynamicPage(name: "sensorPage",  title: "Optional Sensors and Push Buttons", nextPage:"summary", install:false, uninstall:false) {
        def sensorCounter = 1
        state.validatedDoors.each{ door ->
            section("Setup options for " + state.MyQDataPending[door].name){
                input "door${sensorCounter}Sensor",  "capability.contactSensor", required: false, multiple: false, title: state.MyQDataPending[door].name + " Contact Sensor"
                input "prefDoor${sensorCounter}PushButtons", "bool", required: false, title: "Create on/off push buttons?"
            }
            sensorCounter++
        }
        section("Sensor setup"){
        	paragraph "For each door above, you can specify an optional sensor that allows the device type to know whether the door is open or closed. This helps the device function as a switch " +
            	"you can turn on (to open) and off (to close) in other automations and SmartApps."
           	paragraph "Alternatively, you can choose the other option below to have separate additional On and Off push button devices created. This is recommened if you have no sensors but still want a way to open/close the " +
            "garage from SmartTiles and other interfaces like Google Home that can't function with the built-in open/close capability. See wiki for more details"
        }
    }
}

def summary() {
	state.installMsg = ""
    try{
    	initialize()
    }

    //If error thrown during initialize, try to get the line number and display on installation summary page
    catch (e){
		def errorLine = "unknown"
        try{
        	if (logEnable) log.debug e.stackTrace
            def pattern = ( e.stackTrace =~ /groovy.(\d+)./   )
            errorLine = pattern[0][1]
        }
        catch(lineError){}

		if (logEnable) log.debug "Error at line number ${errorLine}: ${e}"
        state.installMsg = "There was a problem updating devices:\n ${e}.\nLine number: ${errorLine}\nLast successful step: ${state.lastSuccessfulStep}"
    }

    return dynamicPage(name: "summary",  title: "Summary", install:true, uninstall:true) {
        section("Installation Details:"){
			paragraph state.installMsg
		}
    }
}

/* Initialization */
def installed() {
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def updated() {
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
	if (logEnable) log.debug "MyQ Lite changes saved."
    unschedule()

    if (door1Sensor && state.validatedDoors){
    	refreshAll()
    	runEvery30Minutes(refreshAll)
    }
    stateCleanup()
}

/* Version Checking */

//Called from scheduler every 3 hours
def updateVersionInfo(){
}

def uninstall(){
    if (logEnable) log.debug "Removing MyQ Devices..."
    childDevices.each {
		try{
			deleteChildDevice(it.deviceNetworkId, true)
		}
		catch (e) {
			if (logEnable) log.debug "Error deleting ${it.deviceNetworkId}: ${e}"
		}
	}
}

def uninstalled() {
	if (logEnable) log.debug "MyQ removal complete."
}


def initialize() {

    if (logEnable) log.debug "Initializing..."
    state.data = state.MyQDataPending
    state.lastSuccessfulStep = ""
    unsubscribe()

    //Check existing installed devices against MyQ data
    verifyChildDeviceIds()



    //Mark sensors onto state door data
    def doorSensorCounter = 1
    state.validatedDoors.each{ door ->
        if (settings["door${doorSensorCounter}Sensor"]){
            state.data[door].sensor = "door${doorSensorCounter}Sensor"
            doorSensorCounter++
        }
    }
    state.lastSuccessfulStep = "Sensor Indexing"

    //Create door devices
    def doorCounter = 1
    state.validatedDoors.each{ door ->
        createChilDevices(door, settings[state.data[door].sensor], state.data[door].name, settings["prefDoor${doorCounter}PushButtons"])
        doorCounter++
    }
    state.lastSuccessfulStep = "Door device creation"


    //Create light devices
    if (lights){
        state.validatedLights = []
        if (lights instanceof List && lights.size() > 1){
            lights.each { lightId ->
                if (state.data[lightId] != null){
                    state.validatedLights.add(lightId)
                }
            }
        }
        else{
            state.validatedLights = lights
        }
        state.validatedLights.each { light ->
            if (light){
                def myQDeviceId = state.data[light].myQDeviceId
                def DNI = [ app.id, "LightController", myQDeviceId ].join('|')
                def lightName = state.data[light].name
                def childLight = getChildDevice(state.data[light].child)

                if (!childLight) {
                    if (logEnable) log.debug "Creating child light device: " + light

                    try{
                        childLight = addChildDevice("brbeaird", "MyQ Light Controller", DNI, getHubID(), ["name": lightName])
                        state.data[myQDeviceId].child = DNI
                        state.installMsg = state.installMsg + lightName + ": created light device. \r\n\r\n"
                    }
                    catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                    {
                        if (logEnable) log.debug "Error! " + e
                        state.installMsg = state.installMsg + lightName + ": problem creating light device. Check your IDE to make sure the brbeaird : MyQ Light Controller device handler is installed and published. \r\n\r\n"
                    }
                }
                else{
                    if (logEnable) log.debug "Light device already exists: " + lightName
                    state.installMsg = state.installMsg + lightName + ": light device already exists. \r\n\r\n"
                }
                if (logEnable) log.debug "Setting ${lightName} status to ${state.data[light].status}"
                childLight.updateDeviceStatus(state.data[light].status)
            }
        }
        state.lastSuccessfulStep = "Light device creation"
    }

    // Remove unselected devices
    getChildDevices().each{ child ->
    	if (logEnable) log.debug "Checking ${child} for deletion"
        def myQDeviceId = child.getMyQDeviceId()
        if (myQDeviceId){
        	if (!(myQDeviceId in state.validatedDoors) && !(myQDeviceId in state.validatedLights)){
            	try{
                	if (logEnable) log.debug "Child ${child} with ID ${myQDeviceId} not found in selected list. Deleting."
                    deleteChildDevice(child.deviceNetworkId, true)
                	if (logEnable) log.debug "Removed old device: ${child}"
                    state.installMsg = state.installMsg + "Removed old device: ${child} \r\n\r\n"
                }
                catch (e)
                {
                    if (logEnable) log.debug "Error trying to delete device: ${child} - ${e}"
                    if (logEnable) log.debug "Device is likely in use in a Routine, or SmartApp (make sure and check Alexa, ActionTiles, etc.)."
                }
            }
        }
    }
    state.lastSuccessfulStep = "Old device removal"

    //Set initial values
    if (state.validatedDoors){
    	syncDoorsWithSensors()
    }
    state.lastSuccessfulStep = "Setting initial values"

    //Subscribe to sensor events
    settings.each{ key, val->
        if (key.contains('Sensor')){
        	subscribe(val, "contact", sensorHandler)
        }
    }
}

def verifyChildDeviceIds(){
	//Try to match existing child devices with latest MyQ data
    childDevices.each { child ->
        def matchingId
        if (child.typeName != 'Momentary Button Tile'){
            //Look for a matching entry in MyQ
            state.data.each { myQId, myQData ->
                if (child.getMyQDeviceId() == myQId){
                    if (logEnable) log.debug "Found matching ID for ${child}"
                    matchingId = myQId
                }

                //If no matching ID, try to match on name
                else if (child.name == myQData.name || child.label == myQData.name){
                    if (logEnable) log.debug "Found matching ID (via name) for ${child}"
                    child.updateMyQDeviceId(myQId)	//Update child to new ID
                    matchingId = myQId
                }
            }

            if (logEnable) log.debug "final matchingid for ${child.name} ${matchingId}"
            if (matchingId){
                state.data[matchingId].child = child.deviceNetworkId
            }
            else{
                if (logEnable) log.debug "WARNING: Existing child ${child} does not seem to have a valid MyQID"
            }
        }
    }
}

def createChilDevices(door, sensor, doorName, prefPushButtons){
    def sensorTypeName = "MyQ Garage Door Opener"
    def noSensorTypeName = "MyQ Garage Door Opener-NoSensor"
    def lockTypeName = "MyQ Lock Door"

    if (door){

    	def myQDeviceId = state.data[door].myQDeviceId
        def myQAccountId = state.data[door].myQAccountId
        def DNI = [ app.id, "GarageDoorOpener", myQDeviceId ].join('|')

        //Has door's child device already been created?
        def existingDev = getChildDevice(state.data[door].child)
        def existingType = existingDev?.typeName

        if (existingDev){
        	if (logEnable) log.debug "Child already exists for " + doorName + ". Sensor name is: " + sensor
            state.installMsg = state.installMsg + doorName + ": door device already exists. \r\n\r\n"
            existingDev.updateMyQDeviceId(myQDeviceId, myQAccountId)

            if (prefUseLockType && existingType != lockTypeName){
                try{
                    if (logEnable) log.debug "Type needs updating to Lock version"
                    existingDev.deviceType = lockTypeName
                    state.installMsg = state.installMsg + doorName + ": changed door device to lock version." + "\r\n\r\n"
                }
                catch(hubitat.exception.NotFoundException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem changing door to no-sensor type. Check your IDE to make sure the brbeaird : " + lockTypeName + " device handler is installed and published. \r\n\r\n"
                }
            }
            else if ((!sensor) && existingType != noSensorTypeName){
            	try{
                    if (logEnable) log.debug "Type needs updating to no-sensor version"
                    existingDev.deviceType = noSensorTypeName
                    state.installMsg = state.installMsg + doorName + ": changed door device to No-sensor version." + "\r\n\r\n"
                }
                catch(hubitat.exception.NotFoundException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem changing door to no-sensor type. Check your IDE to make sure the brbeaird : " + noSensorTypeName + " device handler is installed and published. \r\n\r\n"
                }
            }

            else if (sensor && existingType != sensorTypeName && !prefUseLockType){
            	try{
                    if (logEnable) log.debug "Type needs updating to sensor version"
                    existingDev.deviceType = sensorTypeName
                    state.installMsg = state.installMsg + doorName + ": changed door device to sensor version." + "\r\n\r\n"
                }
                catch(hubitat.exception.NotFoundException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem changing door to sensor type. Check your IDE to make sure the brbeaird : " + sensorTypeName + " device handler is installed and published. \r\n\r\n"
                }
            }
        }
        else{
            if (logEnable) log.debug "Creating child door device " + door
            def childDoor

            if (prefUseLockType){
                try{
                    if (logEnable) log.debug "Creating door with lock type"
                    childDoor = addChildDevice("brbeaird", lockTypeName, DNI, getHubID(), ["name": doorName])
                    childDoor.updateMyQDeviceId(myQDeviceId, myQAccountId)
                    state.installMsg = state.installMsg + doorName + ": created lock device \r\n\r\n"
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem creating door device (lock type). Check your IDE to make sure the brbeaird : " + sensorTypeName + " device handler is installed and published. \r\n\r\n"

                }
            }

            else if (sensor){
                try{
                    if (logEnable) log.debug "Creating door with sensor"
                    childDoor = addChildDevice("brbeaird", sensorTypeName, DNI, getHubID(), ["name": doorName])
                    childDoor.updateMyQDeviceId(myQDeviceId, myQAccountId)
                    state.installMsg = state.installMsg + doorName + ": created door device (sensor version) \r\n\r\n"
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem creating door device (sensor type). Check your IDE to make sure the brbeaird : " + sensorTypeName + " device handler is installed and published. \r\n\r\n"

                }
            }
            else{
                try{
                    if (logEnable) log.debug "Creating door with no sensor"
                    childDoor = addChildDevice("brbeaird", noSensorTypeName, DNI, getHubID(), ["name": doorName])
                    childDoor.updateMyQDeviceId(myQDeviceId, myQAccountId)
                    state.installMsg = state.installMsg + doorName + ": created door device (no-sensor version) \r\n\r\n"
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem creating door device (no-sensor type). Check your IDE to make sure the brbeaird : " + noSensorTypeName + " device handler is installed and published. \r\n\r\n"
                }
            }
            state.data[door].child = childDoor.deviceNetworkId
        }

        //Create push button devices
        if (prefPushButtons){
        	def existingOpenButtonDev = getChildDevice(door + " Opener")
            def existingCloseButtonDev = getChildDevice(door + " Closer")
            if (!existingOpenButtonDev){
                try{
                	def openButton = addChildDevice("brbeaird", "Momentary Button Tile", door + " Opener", getHubID(), [name: doorName + " Opener", label: doorName + " Opener"])
                	state.installMsg = state.installMsg + doorName + ": created push button device. \r\n\r\n"
                	subscribe(openButton, "momentary.pushed", doorButtonOpenHandler)
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    if (logEnable) log.debug "Error! " + e
                    state.installMsg = state.installMsg + doorName + ": problem creating push button device. Check your IDE to make sure the smartthings : Momentary Button Tile device handler is installed and published. \r\n\r\n"
                }
            }
            else{
            	subscribe(existingOpenButtonDev, "momentary.pushed", doorButtonOpenHandler)
                state.installMsg = state.installMsg + doorName + ": push button device already exists. Subscription recreated. \r\n\r\n"
                if (logEnable) log.debug "subscribed to button: " + existingOpenButtonDev
            }

            if (!existingCloseButtonDev){
                try{
                    def closeButton = addChildDevice("brbeaird", "Momentary Button Tile", door + " Closer", getHubID(), [name: doorName + " Closer", label: doorName + " Closer"])
                    subscribe(closeButton, "momentary.pushed", doorButtonCloseHandler)
                }
                catch(com.hubitat.app.exception.UnknownDeviceTypeException e)
                {
                    if (logEnable) log.debug "Error! " + e
                }
            }
            else{
                subscribe(existingCloseButtonDev, "momentary.pushed", doorButtonCloseHandler)
            }
        }

        //Cleanup defunct push button devices if no longer wanted
        else{
        	def pushButtonIDs = [door + " Opener", door + " Closer"]
            def devsToDelete = getChildDevices().findAll { pushButtonIDs.contains(it.deviceNetworkId)}
            if (logEnable) log.debug "button devices to delete: " + devsToDelete
			devsToDelete.each{
            	if (logEnable) log.debug "deleting button: " + it
                try{
                	deleteChildDevice(it.deviceNetworkId, true)
                } catch (e){
                    state.installMsg = state.installMsg + "Warning: unable to delete virtual on/off push button - you'll need to manually remove it. \r\n\r\n"
                    if (logEnable) log.debug "Error trying to delete button " + it + " - " + e
                    if (logEnable) log.debug "Button  is likely in use in a Routine, or SmartApp (make sure and check SmarTiles!)."
                }

            }
        }
    }
}


def syncDoorsWithSensors(child){

    // refresh only the requesting door (makes things a bit more efficient if you have more than 1 door
    /*if (child) {
        def doorMyQId = child.getMyQDeviceId()
        updateDoorStatus(child.device.deviceNetworkId, settings[state.data[doorMyQId].sensor], child)
    }
    //Otherwise, refresh everything
    else{*/
        state.validatedDoors.each { door ->
        	if (logEnable) log.debug "Refreshing ${door} ${state.data[door].child}"
        	if (state.data[door].sensor){
            	updateDoorStatus(state.data[door].child, settings[state.data[door].sensor], '')
            }
        }
    //}
}

def updateDoorStatus(doorDNI, sensor, child){
    try{
        if (logEnable) log.debug "Updating door status: ${doorDNI} ${sensor} ${child}"

        if (!sensor){//If we got here somehow without a sensor, bail out
        	if (logEnable) log.debug "Warning: no sensor found for ${doorDNI}"
            return 0}

		if (!doorDNI){
        	if (logEnable) log.debug "Invalid doorDNI for sensor ${sensor} ${child}"
            return 0
        }

        //Get door to update and set the new value
        def doorToUpdate = getChildDevice(doorDNI)
        def doorName = "unknown"
        if (state.data[doorDNI]){doorName = state.data[doorDNI].name}

        //Get current sensor value
        def currentSensorValue = "unknown"
        currentSensorValue = sensor.latestValue("contact")
        def currentDoorState = doorToUpdate.latestValue("door")
        doorToUpdate.updateSensorBattery(sensor.latestValue("battery"))

        //If sensor and door are out of sync, update the door
		if (currentDoorState != currentSensorValue){
        	if (logEnable) log.debug "Updating ${doorName} as ${currentSensorValue} from sensor ${sensor}"
            doorToUpdate.updateDeviceStatus(currentSensorValue)
        	doorToUpdate.updateDeviceSensor("${sensor} is ${currentSensorValue}")

            //Write to child log if this was initiated from one of the doors
            if (child){child.log("Updating as ${currentSensorValue} from sensor ${sensor}")}

            //Get latest activity timestamp for the sensor (data saved for up to a week)
            def eventsSinceYesterday = sensor.eventsSince(new Date() - 7)
            def latestEvent = eventsSinceYesterday[0]?.date

            //Update timestamp
            if (latestEvent){
            	doorToUpdate.updateDeviceLastActivity(latestEvent)
            }
            else{	//If the door has been inactive for more than a week, timestamp data will be null. Keep current value in that case.
            	timeStampLogText = "Door: " + doorName + ": Null timestamp detected "  + " -  from sensor " + sensor + " . Keeping current value."
            }
        }
    }catch (e) {
        if (logEnable) log.debug "Error updating door: ${doorDNI}: ${e}"
    }
}

def refresh(child){
    def door = child.device.deviceNetworkId
    def doorName = state.data[child.getMyQDeviceId()].name
    child.log("refresh called from " + doorName + ' (' + door + ')')
    syncDoorsWithSensors(child)
}

def refreshAll(){
    syncDoorsWithSensors()
}

def refreshAll(evt){
	refreshAll()
}

def sensorHandler(evt) {
    if (logEnable) log.debug "Sensor change detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId

    state.validatedDoors.each{ door ->
        if (settings[state.data[door].sensor]?.id?.toInteger() == evt.deviceId)
            updateDoorStatus(state.data[door].child, settings[state.data[door].sensor], null)
    }
}

def doorButtonOpenHandler(evt) {
    try{
        if (logEnable) log.debug "Door open button push detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId + " DNI: " + evt.getDevice().deviceNetworkId
        evt.getDevice().off()
        def myQDeviceId = evt.getDevice().deviceNetworkId.replace(" Opener", "")
        def doorDevice = getChildDevice(state.data[myQDeviceId].child)
        doorDevice.open()
    }catch(e){
    	def errMsg = "Warning: MyQ Open button command failed - ${e}"
        log.error errMsg
    }
}

def doorButtonCloseHandler(evt) {
	try{
		if (logEnable) log.debug "Door close button push detected: Event name  " + evt.name + " value: " + evt.value   + " deviceID: " + evt.deviceId + " DNI: " + evt.getDevice().deviceNetworkId
        evt.getDevice().off()
        def myQDeviceId = evt.getDevice().deviceNetworkId.replace(" Closer", "")
        def doorDevice = getChildDevice(state.data[myQDeviceId].child)
        doorDevice.close()
	}catch(e){
    	def errMsg = "Warning: MyQ Close button command failed - ${e}"
        log.error errMsg
    }
}


def getSelectedDevices( settingsName ) {
	def selectedDevices = []
	(!settings.get(settingsName))?:((settings.get(settingsName)?.getAt(0)?.size() > 1)  ? settings.get(settingsName)?.each { selectedDevices.add(it) } : selectedDevices.add(settings.get(settingsName)))
	return selectedDevices
}

/* Access Management */
private forceLogin() {
	//Reset token and expiry
    log.warn "forceLogin: Refreshing login token"
	state.session = [ brandID: 0, brandName: settings.brand, securityToken: null, refreshToken: null, expiration: 0 ]
	return doLogin()
}

private login() {
	if (now() > state.session.expiration){
    	log.warn "Token has expired. Logging in again."
        doLogin()
    }
    else{
    	return true;
    }
}

private doLogin() {
    if (state.session.expiration == 0)
        return doUserNameAuth()
    else {
        if (doRefreshTokenAuth())
            return true
        else {
            state.session = [ brandID: 0, brandName: settings.brand, securityToken: null, refreshToken: null, expiration: 0 ]
            return doUserNameAuth()
        }
    }
}

//Get devices listed on your MyQ account
private getMyQDevices() {
	state.MyQDataPending = [:]
    state.unsupportedList = []

    def accounts = null
    try {

    
    httpGet([ 
        uri: "https://accounts.myq-cloud.com",
        path: "/api/v6.0/accounts", 
        headers: getMyQHeaders()
    ]) { resp ->
        accounts = resp.data.accounts
    }
    }
    catch (e) {
        log.debug "o"
    }

    if (!accounts){
        log.warn "No accounts found."
        return
    }

    accounts.each { account ->
        def devices = null
        httpGet([ 
            uri: "https://devices.myq-cloud.com",
            path: "/api/v5.2/Accounts/${account.id}/Devices", headers: getMyQHeaders()]) { resp ->
                devices = resp.data.items
        }

        devices.each { device ->
            // 2 = garage door, 5 = gate, 7 = MyQGarage(no gateway), 9 = commercial door, 17 = Garage Door Opener WGDO
            //if (device.MyQDeviceTypeId == 2||device.MyQDeviceTypeId == 5||device.MyQDeviceTypeId == 7||device.MyQDeviceTypeId == 17||device.MyQDeviceTypeId == 9) {
            if (device.device_family == "garagedoor") {
                if (logEnable) log.debug "Found door: ${device.name}"
                def dni = device.serial_number
                def description = device.name
                def doorState = device.state.door_state
                def updatedTime = device.last_update

                //Ignore any doors with blank descriptions
                if (description != ''){
                    if (logEnable) log.debug "Got valid door: ${description} type: ${device.device_family} status: ${doorState} type: ${device.device_type}"
                    //log.debug "Storing door info: " + description + "type: " + device.device_family + " status: " + doorState +  " type: " + device.device_type
                    state.MyQDataPending[dni] = [ status: doorState, lastAction: updatedTime, name: description, typeId: device.MyQDeviceTypeId, typeName: 'door', sensor: '', myQDeviceId: device.serial_number, myQAccountId: account.id]
                }
                else if (logEnable) 
                    log.debug "Door " + device.MyQDeviceId + " has blank desc field. This is unusual..."
            }

            //Lights
            else if (device.device_family == "lamp") {
                def dni = device.serial_number
                def description = device.name
                def lightState = device.state.lamp_state
                def updatedTime = device.state.last_update

                //Ignore any lights with blank descriptions
                if (description && description != ''){
                    if (logEnable) log.debug "Got valid light: ${description} type: ${device.device_family} status: ${lightState} type: ${device.device_type}"
                    state.MyQDataPending[dni] = [ status: lightState, lastAction: updatedTime, name: description, typeName: 'light', type: device.MyQDeviceTypeId, myQDeviceId: device.serial_number, myQAccountId: account.id ]
                }
            }

            //Unsupported devices
            else{
                state.unsupportedList.add([name: device.name, typeId: device.device_family, typeName: device.device_type])
            }
        }
    }
}

def getHubID(){
    return 1234
}

/* API Methods */
private getMyQHeaders() {
	return [
        "Authorization": "Bearer ${state.session.securityToken}"
    ]
}

// HTTP PUT call (Send commands)
private apiPut(apiPath, apiBody = [], actionText = "") {
    if (!login()){
        log.error "Unable to complete PUT, login failed"
        return false
    }
    def result = false
    try {
        httpPut([ 
                uri: COMMAND_HOSTNAME,
                path: apiPath, 
                headers: getMyQHeaders()
            ]) { resp ->
            if (resp.status != 200 && resp.status != 204 && resp.status != 202) {
                log.warn "Unexpected command response - ${resp.status} ${resp.data}"
            }
            result = true
        }
    } catch (e)	{
        if (e.response.data?.description == "Device already in desired state."){
            log.debug "Device already in desired state. Command ignored."
        	result = true
		}
        result = false
    }
    return result
}

def sendDoorCommand(myQDeviceId, myQAccountId, command) {
	if (!myQAccountId){
        myQAccountId = state.session.accountId  //Bandaid for people who haven't tapped through the modify menu yet to assign accountId to door device
    }
    state.lastCommandSent = now()
    return apiPut("/api/v5.2/Accounts/${myQAccountId}/door_openers/${myQDeviceId}/${command}")
}

def sendLampCommand(myQDeviceId, myQAccountId, command) {
	state.lastCommandSent = now()
    return apiPut("/api/v5.2/Accounts/${myQAccountId}/lamps/${myQDeviceId}/${command}")
}

//Transition for people who have not yet clicked through "modify devices" steps
def getDefaultAccountId(){
	return state.session.accountId
}


//Remove old unused pieces of state
def stateCleanup(){
    if (state.latestDoorNoSensorVersion){state.remove('latestDoorNoSensorVersion')}
    if (state.latestDoorVersion){state.remove('latestDoorVersion')}
    if (state.latestLightVersion){state.remove('latestLightVersion')}
    if (state.latestSmartAppVersion){state.remove('latestSmartAppVersion')}
    if (state.thisDoorNoSensorVersion){state.remove('thisDoorNoSensorVersion')}
    if (state.thisDoorVersion){state.remove('thisDoorVersion')}
    if (state.thisLightVersion){state.remove('thisLightVersion')}
    if (state.thisSmartAppVersion){state.remove('thisSmartAppVersion')}
    if (state.versionWarning){state.remove('versionWarning')}
    if (state.polling){state.remove('polling')}
}

//Available to be called from child devices for special logging
def notify(message){

}

// PKCE methods
def getPKCEVerifierCode() {
    return UUID.randomUUID().toString() + UUID.randomUUID().toString()
}

def generatePKCEChallenge(code) {
    def digest = MessageDigest.getInstance("SHA-256")   
    def hash = digest.digest(code.getBytes("UTF-8"))

    return base64urlencode(hash)
}

// Adapted from RFC7636
def base64urlencode(arg)
{
    s = arg.encodeBase64().toString()
    s = s.split('=')[0] // Remove any trailing '='s
    s = s.replace('+', '-') // 62nd char of encoding
    s = s.replace('/', '_') // 63rd char of encoding
    return s
}

// URL parsing methods
def splitUri(uri, htmlEncoded = false) {
    def parts = uri?.split(/\?/)
    def queryParts = null
    if (htmlEncoded)
        queryParts = parts[1].split('&amp;')
    else 
        queryParts = parts[1].split('&')

    def query = [:]

    for (kvp in queryParts) {
        def splitkvp = kvp.split('=')
        query."${splitkvp[0]}" = java.net.URLDecoder.decode(splitkvp[1])
    }
    
    return [uri: parts[0], query: query]
}

def doUserNameAuth() {
    def result = true
    try {
        def code = getPKCEVerifierCode()        
        def token = ""
        def cookie = ""
        def location = ""
        def authCookie = ""
 
        httpGet([ 
            uri: AUTH_HOSTNAME, 
            path: "/connect/authorize", 
            headers: [
                "User-Agent": "null"
            ],
            query: [
                client_id: CLIENT_ID,
                code_challenge: generatePKCEChallenge(code),
                code_challenge_method: "S256",
                response_type: "code",
                scope: "MyQ_Residential offline_access",
                redirect_uri: REDIRECT_URI
            ],
            textParser: true
        ]) { resp ->            
			if (resp.status == 200) {
                def responseText = resp.data.text
                token = (responseText =~ /input name="__RequestVerificationToken" type="hidden" value="(.*?)"/)[0][1]
                postUrl = (responseText =~ /form action="(.*?)" method="post"/)[0][1]
                cookie = resp?.headers?.'Set-Cookie'.split(';')[0]
            }
            else {
                log.error "Error retrieving verification token: ${resp.status}"
                result = false
            }
        }

        if (result == false)
            return false
        def uri = splitUri(postUrl, true)

        httpPost([
            uri: AUTH_HOSTNAME,
            path: uri.uri,
            query: uri.query,
            headers: [
                "User-Agent": "null",
                "Cookie": cookie
            ],
            body: [
                "Email": settings.username,
                "Password": settings.password,
                "__RequestVerificationToken": token
            ]
        ]) { resp -> 
            if (resp.status == 302 || resp.status == 200) {
                location = resp.headers.Location
                for (header in resp.headers) {
                    if (header.name == "Set-Cookie") {
                        authCookie += "${header.value}; "
                    }
                }
            }
            else {
                log.error "Error logging in: ${resp.status}"
                result = false
            }
        }
        if (result == false)
            return false

        uri = splitUri(location)

        def pkceResponse = ""
        httpGet([
            uri: AUTH_HOSTNAME,
            path: uri.uri,
            query: uri.query,
            headers: [
                "Cookie": authCookie
            ],
            followRedirects: false
        ]) { resp ->
            if (resp.status == 302)
                pkceResponse = resp.headers.Location
            else {
                log.error "Error validating PKCE code: ${resp.status}"
                result = false
            }
        }
        
        if (result == false)
            return false

        pkceUrlParts = splitUri(pkceResponse)

        httpPost([
            uri: AUTH_HOSTNAME,
            path: "/connect/token",
            headers: [
                "User-Agent": "null"
            ],
            body: [
                client_id: CLIENT_ID,
                client_secret: CLIENT_SECRET,
                code: pkceUrlParts.query.code,
                code_verifier: code,
                grant_type: "authorization_code",
                redirect_uri: REDIRECT_URI,
                scope: pkceUrlParts.query.scope
            ]
        ]) { resp -> 
            if (resp.status == 200) {
                state.session.securityToken = resp.data.access_token
                state.session.refreshToken = resp.data.refresh_token
                state.session.expiration = now() + (3600*1000) - 10000
            }
            else {
                log.error "Error retrieving auth token: ${resp.status}"
                result = false
            }
        }
    }
    catch (e) {
        log.debug e
        return false
    }
    return result
}

def doRefreshTokenAuth() {
        def result = true
        httpPost([
                uri: AUTH_HOSTNAME, 
                path: "/connect/token", 
                headers: ["User-Agent": "null"], 
                body: [
                    "client_id": CLIENT_ID,
                    "client_secret": CLIENT_SECRET,
                    "grant_type": "refresh_token",
                    "redirect_uri": REDIRECT_URI,
                    "scope": "MyQ_Residential offline_access",
                    "refresh_token": state.session.refreshToken
                ] 
            ]) { resp ->
                if (resp.status == 200) {
                    state.session.securityToken = resp.data.access_token
                    state.session.refreshToken = resp.data.refresh_token
                    state.session.expiration = now() + (resp.data.expires_in * 1000) - 10000
                    result = true
                } else {
                    log.error "Unknown LOGIN POST status: ${resp.status} data: ${resp.data}"
                    state.loginMessage = "${resp.status}-${resp.data}"
                    state.session.expiration = now() - 1000
                    result = false
                }
        }
        return result
}