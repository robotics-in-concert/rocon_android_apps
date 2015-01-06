# Beacon Awareness

App launcher by beacon

### Pre-required
* Android device supported NFC and Bluetooth Low Energy
    * Nexus5, Nexus9 
* Beacon Awareness App
* NFC tag and Rocon NFC tags writer app
    * For more easliy setting
* Demo Concert
    * https://github.com/robotics-in-concert/rocon_demos

### Usage
* Writing Beacon Awareness App infomation on NFC Tag
  * Add following interaction info about Beacon Awareness App in interaction file
      
      ```
      - name: com.github.robotics_in_concert.rocon_android_apps.beacon_awareness.BeaconAwarenessMainActivity
        role: 'Role'
        compatibility: rocon:/*/*/*/jellybean|ice_cream_sandwich|kitkat
        display_name: Beacon Awareness
        description: Beacon Awareness 
        max: -1
        remappings:
          - remap_from: beacons
            remap_to: /beacons
          - remap_from: start_app
            remap_to: /start_app
        parameters:
          beacon_map: 
            - beacon_name: black
              beacon_mac: D0:FF:50:66:93:3F
              app_list: "[1111,2222]"
            - beacon_name: my beacon
              beacon_mac: AA:BB:CC:DD:EE:FF
              app_list: "[3333,4444]"
      ```
    * beacon map is information regarding used beacon.
      * beacon_name : human frinedly name
      * beacon_mac : unique id
      * app_list : A list of launchable application. It is composed as app hash.
      
  * Get the app hash of Beacon Awareness app by ```rocon_interaction```
  * Launch Rocon NFC tags writer app
      * You can downlaod [this](http://files.yujinrobot.com/android/apks/rocon_nfc_writer_ver_2nd_milestone.apk). It will be uploaded Android App Market 
  * Fill out the SSID, Password, Concert ROS Master URI. And then write app hash of Beacon Awareness app in App hash
* Tagging and check the setting Info.
  * Tagging NFC tag after completing record interaction information.
  * After launch the app, you can check the interaction information as tapping rocon image.
* Start Service
  * Hit the ```SERVICE START``` button.
  * Beacon Awareness App shows the popup for starting app when your device is near by beacon.  
  * If you choose ```Yes```, Beacon Awareness App try to connect concert, and lauch the app.

### Reference
* App hash
    * A nine-digit number for representing interaction information.
    * It can get by following command when concert is launched.
      
      ```
      > rocon_interaction
      ```
