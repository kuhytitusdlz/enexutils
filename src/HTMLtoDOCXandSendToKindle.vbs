Option Explicit

Main
Private Sub Main
Dim arguments

    Set arguments = WScript.Arguments
	Dim arr(), arr2, i
	Dim fName
	
    If arguments.Count = 0 Then
    	ReDim Preserve arr(2) ' расширить массив до 2х с сохранением прежних значений (preserve)
		arr(0) = ".\enex"
		arr(1) = ".html"
		'SearchFiles(arr)
		
		arr2 = SearchFiles(arr)
		for i=LBound(arr2) to UBound(arr2)
			'msgBox arr2(i)
			fName = ConvertHtml(arr2(i))
			EmailSend(fName)
			DeleteFiles(fName)
		next
    	'WScript.Echo "Missing file argument."
    Else
		'ConvertToHtml arguments(0)
		'ConvertHtml arguments(0)
    End If

End Sub

Function ConvertHtml(documentFileName)
    Const wdFormatDocument                    =  0
    Const wdFormatDocument97                  =  0
    Const wdFormatDocumentDefault             = 16
    Const wdFormatDOSText                     =  4
    Const wdFormatDOSTextLineBreaks           =  5
    Const wdFormatEncodedText                 =  7
    Const wdFormatFilteredHTML                = 10
    Const wdFormatFlatXML                     = 19
    Const wdFormatFlatXMLMacroEnabled         = 20
    Const wdFormatFlatXMLTemplate             = 21
    Const wdFormatFlatXMLTemplateMacroEnabled = 22
    Const wdFormatHTML                        =  8
    Const wdFormatPDF                         = 17
    Const wdFormatRTF                         =  6
    Const wdFormatTemplate                    =  1
    Const wdFormatTemplate97                  =  1
    Const wdFormatText                        =  2
    Const wdFormatTextLineBreaks              =  3
    Const wdFormatUnicodeText                 =  7
    Const wdFormatWebArchive                  =  9
    Const wdFormatXML                         = 11
    Const wdFormatXMLDocument                 = 12
    Const wdFormatXMLDocumentMacroEnabled     = 13
    Const wdFormatXMLTemplate                 = 14
    Const wdFormatXMLTemplateMacroEnabled     = 15
    Const wdFormatXPS                         = 18
    Const wdFormatOfficeDocumentTemplate      = 23
    Const wdFormatMediaWiki                   = 24
	Dim fso
	Dim wordApplication
	Dim newDocument
	Dim SaveFileName
    On Error Resume Next

    Set fso = WScript.CreateObject("Scripting.FileSystemObject")

    documentFileName = fso.GetAbsolutePathName(documentFileName)
    If Not fso.FileExists(documentFileName) Then
    	WScript.Echo "The file '" & documentFileName & "' does not exist."
    	WScript.Quit
    End If

    Set wordApplication = WScript.CreateObject("Word.Application")
    If Err.Number <> 0 Then
    	Select Case Err.Number
    	Case &H80020009
    		WScript.Echo "Word not installed properly."
    	Case Else
    		ShowDefaultErrorMsg
    	End Select
    	wordApplication.Quit
    	WScript.Quit
    End If
	' True: make Word visible; False: invisible
    wordApplication.Visible = False
	
	' Construct a file name which is the same as the original file, but with a different extension.
    SaveFileName = Left(documentFileName, InStrRev(documentFileName, ".")) & "docx"
	' Check if the Word document exists
	Dim objFSO
	Set objFSO = CreateObject("Scripting.FileSystemObject")
	If objFSO.FileExists( SaveFileName ) Then
		WScript.Echo "FILE ERROR: The file exist: " & SaveFileName & vbCrLf
		' Close Word
		wordApplication.Quit
		WScript.Quit
		Exit Function
	End If
	
    Set newDocument = wordApplication.Documents.Open(documentFileName, False)
    If Err.Number <> 0 Then
    	Select Case Err.Number
    	Case Else
    		ShowDefaultErrorMsg
    	End Select
    	wordApplication.Quit
    	WScript.Quit
    End If

	
	Dim Shape 'As InlineShape
	For Each Shape In newDocument.InlineShapes
		If (Shape.Type = wdInlineShapeLinkedPicture) Or (Shape.Type = wdInlineShapeLinkedPictureHorizontalLine) Then
		  Shape.LinkFormat.SavePictureWithDocument = True
		  Shape.LinkFormat.BreakLink ' отв€зать ссылки на картинки
		End If
	Next    
	newDocument.SaveAs SaveFileName, wdFormatXMLDocument
	newDocument.Close
    wordApplication.Quit
	ConvertHtml = SaveFileName
End Function

Private Sub ConvertToHtml(documentFileName)
	Const wdFormatHTML = 8
	Dim fso
	Dim wordApplication
	Dim newDocument
	Dim htmlFileName
    On Error Resume Next

    Set fso = WScript.CreateObject("Scripting.FileSystemObject")

    documentFileName = fso.GetAbsolutePathName(documentFileName)

    If Not fso.FileExists(documentFileName) Then
    	WScript.Echo "The file '" & documentFileName & "' does not exist."
    	WScript.Quit
    End If

    Set wordApplication = WScript.CreateObject("Word.Application")

    If Err.Number <> 0 Then
    	Select Case Err.Number
    	Case &H80020009
    		WScript.Echo "Word not installed properly."
    	Case Else
    		ShowDefaultErrorMsg
    	End Select
    	wordApplication.Quit
    	WScript.Quit
    End If

    Set newDocument = wordApplication.Documents.Open(documentFileName, False)

    If Err.Number <> 0 Then
    	Select Case Err.Number
    	Case Else
    		ShowDefaultErrorMsg
    	End Select
    	wordApplication.Quit
    	WScript.Quit
    End If

    ' Construct a file name which is the same as the original file, but with a different extension.
    htmlFileName = Left(documentFileName, InStrRev(documentFileName, ".")) & "htm"

    newDocument.SaveAs htmlFileName, wdFormatHTML

    newDocument.Close

    wordApplication.Quit
End Sub

Function SearchFiles(arr)
	'on error resume next
	
	'sPath,vKillDate,arFilesToKill,bIncludeSubFolders
	
	'arr(0) - каталог, в котором искать
	'arr(1) - строка, в которой искать
	'arr(2) - если true, то также ищем в подпапках
	
	'If arr(3) = true then
	'End if
	'for i=LBound(arr) to UBound(arr)
	'next
	Dim objFSO, folder, files, fCount, file, count, arr2()
	Set objFSO = CreateObject("Scripting.FileSystemObject")
	'Set objFile = objFSO.GetFile("C:\Scripts\Test.txt")
	'objFSO.GetFileName(objFile)
	
	'msgBox arr(0) & vbCrLf & arr(1)
	
	set folder = objFSO.getfolder(arr(0))
	set files = folder.files
	count = -1 ' используем дл€ расширени€ массива
	fCount = folder.files.Count	
	for each file in files
		'msgBox Instr(objFSO.GetFileName(file), arr(1), 1)
		If (Instr(1, objFSO.GetFileName(file), arr(1), 1) <> 0) then ' ищем с первого символа, без учета регистра
			'msgBox objFSO.GetFileName(file) & vbCrLf & objFSO.GetBaseName(file) & vbCrLf & objFSO.GetExtensionName(file)
			count = count + 1 ' если нашли файл, то расшир€ем массив
			ReDim Preserve arr2(count)
			set arr2(count) = file ' сохран€ем в массив путь до найденного файла
			'WScript.Quit
		End if
		'
		'if not isnull(dtlastmodified) Then
		'	'WriteToLog("INFO: найденные файлы " & file)
		'	if dtlastmodified < vKillDate then
		'		count = ubound(arFilesToKill) + 1
		'		redim preserve arFilesToKill(count)
		'		set arFilesToKill(count) = file
		'		'WriteToLog("INFO: найденные файлы удовлетвор€ющие условию " & file)
		'	end if
		'end if
	next
	
	SearchFiles = arr2
	Erase arr ' очистить массив
	Erase arr2
'	' проверить файлы в подпапках	
'	if bIncludeSubFolders then
'		for each fldr in folder.subfolders
'		  SelectFiles fldr.path,vKillDate,arFilesToKill,true
'		next
'	end if
	
End Function
 
Private Sub ShowDefaultErrorMsg
    WScript.Echo "Error #" & CStr(Err.Number) & vbNewLine & vbNewLine & Err.Description
End Sub

Function EmailSend(attach)
	Dim EmailSubject, EmailBody, objMessage
	EmailSubject				= "Sending Email by CDO"
	EmailBody					= "This is the body of a message sent via" & vbCRLF & _
									"a CDO.Message object using SMTP authentication, with port 465."

	Const EmailFrom				= "uytuytuytu@gmail.com"
	Const EmailFromName			= "My Very Own Name"
	Const EmailTo				= "hfjtffy@kindle.com"
	Const SMTPServer			= "smtp.gmail.com"
	Const SMTPLogon				= "kuhkuhiu@gmail.com"
	Const SMTPPassword			= "pass"
	Const SMTPSSL				= True
	Const SMTPPort				= 465
	Const cdoSendUsingPickup	= 1		'Send message using local SMTP service pickup directory.
	Const cdoSendUsingPort		= 2		'Send the message using SMTP over TCP/IP networking.
	Const cdoAnonymous			= 0		' No authentication
	Const cdoBasic				= 1		' BASIC clear text authentication
	Const cdoNTLM				= 2		' NTLM, Microsoft proprietary authentication

	' First, create the message

	Set objMessage				= CreateObject("CDO.Message")
	objMessage.Subject			= EmailSubject
	objMessage.From				= """" & EmailFromName & """ <" & EmailFrom & ">"
	objMessage.To				= EmailTo
	objMessage.TextBody			= EmailBody
	objMessage.AddAttachment	attach	'NOTE: DO NOT USE AN "=" SIGN AFTER "AddAttachment"
	
	' Second, configure the server
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/sendusing")				= 2
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/smtpserver")				= SMTPServer
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/smtpauthenticate")			= cdoBasic
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/sendusername")				= SMTPLogon
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/sendpassword")				= SMTPPassword
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/smtpserverport")			= SMTPPort
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/smtpusessl")				= SMTPSSL
	objMessage.Configuration.Fields.Item _
	("http://schemas.microsoft.com/cdo/configuration/smtpconnectiontimeout")	= 60

	objMessage.Configuration.Fields.Update
	'Now send the message!
	On Error Resume Next
	objMessage.Send

	If Err.Number <> 0 Then
		MsgBox Err.Description,16,"Error Sending Mail"
	Else 
		'MsgBox "Mail was successfully sent !",64,"Information"
	End If
End Function

Function DeleteFiles(fName)
'
'	удалить файлы .docx, .html, .enex и каталог "_folder"
'
'
	dim filesys
	Set filesys = CreateObject("Scripting.FileSystemObject")
	If filesys.FileExists(fName) Then ' если .docx существует
		filesys.DeleteFile fName
		
		fName = Left(fName, InStrRev(fName, ".")) & "html"
		If filesys.FileExists(fName) Then
			filesys.DeleteFile fName
		End If 
		fName = Left(fName, InStrRev(fName, ".")) & "enex"
		If filesys.FileExists(fName) Then
			filesys.DeleteFile fName
		End If 
		fName = Left(fName, InStrRev(fName, ".") - 1) & "_files"
		If filesys.FolderExists(fName) Then 
			filesys.DeleteFolder fName
		End If 
	End If 

End Function
