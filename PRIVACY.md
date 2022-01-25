# Snapdrop for Android Privacy Policy

"Snapdrop for Android" is an open source project which pursues the goal of providing an easy-to-use cross-platform file sharing solution, which is integrated well into the Android™ system.

To be able to provide our services, the app is powered by the open-source web app https://snapdrop.net (Hereinafter simply called "Snapdrop"). 

## Server connections

To allow discovering of other devices running Snapdrop in your local network, the app makes requests to the Snapdrop server while the app is being executed in the foreground. The server will progress your public IP address and will create a "room" for every IP. If you open Snapdrop on multiple devices in the same network, they will share the same public IP and room, and will therefore "see" each other. However, none of your data will be stored permanently on any server. The data is only kept in RAM for the time the app is opened.

## Users rights

We cannot provide you an option to delete your user data, because we simply store nothing permanently. All data we process is required for the service to work and is used for nothing else than as described here. We do not share any personal data with third parties. 
If you don't want to use our app any longer, just uninstall the app from your device. This will delete all app data (except user files which where received). 

## Permissions 

This app requires storage access on your device. This permission is necessary for using this app, if you want to share (send) and receive (download) files. We will not process your files (even not locally!), except the ones you have explicitly selected for sharing. 
Furthermore, the app can read the WiFi state. This status info is only progressed locally and will not leave your device. 

## Progressed data

The following data will be progressed and is visible for other devices ("peers") in the same network:
- A randomly generated UUID
- A pseudo random user displayable device identifier generated out of the UUID
- The device model name (or a custom name if set)

When you select files to share with a specific peer, a WebRTC connection will be established. WebRTC needs a server for establishing the connection, but the server is not involved in the file transfer itself. None of your files are ever sent to any server, but are transferred directly peer-to-peer between your devices inside your local network. Furthermore, WebRTC encrypts all files on transmission, so nobody could read them anyway.

## Exceptions

In addition, this app allows configuring a custom snapdrop server instance. However, we cannot guarantee about the privacy of third party snapdrop instances you have configured.

## Support email

We provide you our support email address: snapdrop-android[at]googlegroups.com

All emails sent to that address will automatically be forwarded to our core developers. We do not share your emails with third parties. Support emails aren't deleted automatically by default. If you want, that we delete your previous emails, please let us know.
