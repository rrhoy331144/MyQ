For general info on installation of Custom Apps please refer to https://docs.hubitat.com/index.php?title=How_to_Install_Custom_Apps 


1. Open up Hubitat Environment (HE) and go "Apps Code"
2. Select "New App" in the upper right-hand corner.
3. Go to the smartapps/brbeaird folder on GitHub, open myq-lite.src, and copy all of the code.
4. The app will take some time to load. 
5. Open up the "Drivers Code" on HE.
6. Go to the devicetypes/brbeaird folder on GitHub. Reference the list below for the drivers you will need to install. For
each of those drivers, complete the following steps.
7. Select "New Driver" in the upper right-hand corner
8. Open the relevant driver on the GitHub folder and copy all of the code.
9. Paste the code into the window on HE. Select Save.
10. When done installing the required drivers, go the the HE "Apps" page.
11. "Select Add User App" and select the app "MyQ Lite" from the list.
12. Choose "Tap to midify account" and provide credentials.
13. Tap to modify devices. Choose devices to be added. DO NOT PRESS Advanced. And press Next.
14. Add a contact sensor if required and choose Create on/off push button if required.

Hubitat Driver File
- For garage door with tilt/door sensor install myq-garage-door-opener.src driver.
- For garage door with no tilt/door sensor install momentary-button-tile.src and myq-garage-door-opener-nosensor.src drivers.
- For MyQ lights install myq-light-controller.src driver.