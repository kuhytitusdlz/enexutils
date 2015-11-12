Option Explicit

Main
Private Sub Main
	Dim objShell : Set objShell = CreateObject( "WScript.Shell" )
	Dim command
    Const java = """c:\Program Files (x86)\Java\jre1.8.0_65\bin\java.exe"" -Dfile.encoding=UTF-8"
	
	command =  java                                                                                                       &" "&_
	           "-classpath .\build\classes;evernote-api-1.25.1.jar;jdom-2.0.6.jar enexDownload"                                         &" "&_
	           """S=s9:U=19876870:E=158765765765b:C=1565765140:P=1cd:A=en-devtoken:V=2:H=8c619878687056789538e20dbd65f400b""" &" "&_
	           """tag:kindle created:day-30"""                                                                            &" "&_
	           """./enex"""                                                                                               &" "&_
	           """PRODUCTION"""
	objShell.exec(command)	' скачать новые .enex-файлы
	Wscript.Sleep 30000		' поскольку vbs не ждет пока отработает java, то ждем сами
	
	command =  java                                        &" "&_
	           "-classpath .\build\classes;jdom-2.0.6.jar enexToHtml"    &" "&_
	           """./enex"""
	objShell.exec(command)
	Wscript.Sleep 30000		' поскольку vbs не ждет пока отработает java, то ждем сами

	objShell.exec("cscript HTMLtoDOCXandSendToKindle.vbs")
	
	Set objShell = Nothing
End Sub
