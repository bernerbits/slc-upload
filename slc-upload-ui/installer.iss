; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "PlansAnywhere File Transport Manager"
#define MyAppVersion "1.0"
#define MyAppPublisher "Derek Berner"
#define MyAppURL "http://bernerbits.net/"
#define MyAppExeName "pa-transport.exe"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{02872EB2-D0DB-4A89-BFED-C0032956F814}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
OutputBaseFilename=setup
Compression=lzma
SolidCompression=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "C:\Users\Derek\Projects\avolve\slc-upload\slc-upload-ui\build\launch4j\pa-transport.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "C:\Users\Derek\Projects\avolve\slc-upload\slc-upload-ui\build\launch4j\jre8\*"; DestDir: "{app}\jre8"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "C:\Users\Derek\Projects\avolve\slc-upload\slc-upload-ui\build\launch4j\lib\*"; DestDir: "{app}\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

