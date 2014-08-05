SeedRnd MilliSecs()

Include "pregame.bb"
init_map()

Include "other_defines.bb"
Include "items.bb"

AppTitle "Map Editor 14"

Global M_WINDOW=CreateWindow("Space Station 14",200,200,640,640,0)
Global M_CANVAS=CreateCanvas(0,0,512,512,M_WINDOW)
SetBuffer CanvasBuffer()

Dim message_log$(4)
Global font=LoadFont("Arial",12)
SetFont font

Global CAMX = 119 ; 128;128 is the spawn point
Global CAMY = 121

Global map_edit_choice=0

DebugLog "All clear"

Repeat
	Cls
	
	renderScreen()
	
	renderGUI()
	
	editMap()
	readUpdates()
	updateInput()
	Flip
Forever

Function editMap()
	If MouseHit(3)
		MouseTX = (MouseX()/32+camX)
		MouseTY = (MouseY()/32+camY)
		
		;map(MouseTX,MouseTY)\T_TYPE = map_edit_choice
		;map(MouseTX,MouseTY)\T_FRAME = 0
		NEW_OBJ(OBJ_LIGHT,MouseTX,MouseTY)
	EndIf
	
	If KeyHit(57) ;spacebar
		Select map_edit_choice
			Case T_WALL
				map_edit_choice = T_FLOOR
			Case T_FLOOR
				map_edit_choice = T_SPACE
			Case T_SPACE
				map_edit_choice = T_WALL
		End Select
	EndIf
	
	If KeyHit(31) ;'S'
		saveMap()
	EndIf
End Function

Function saveMap()
	Local F = WriteFile("map.ss")
	For x=0 To 255
		For y=0 To 255
			WriteByte F, map(x,y)\T_TYPE
			WriteByte F, map(x,y)\T_FRAME
		Next
	Next
	
	Local OBJ_COUNT=0
	For O.obj=Each obj
		OBJ_COUNT=OBJ_COUNT+1
	Next
	If OBJ_COUNT>0 Then WriteInt F, OBJ_COUNT
	For O.obj=Each obj
		WriteByte F, O\x
		WriteByte F, O\y
		WriteByte F, O\B_TYPE
		WriteByte F, O\B_FRAME
	Next
	
	CloseFile F
End Function

Function connect(IP$,PORT)
	GAME = OpenTCPStream(IP$,PORT)
	If Not GAME Then RuntimeError("Couldn't connect to "+IP$+":"+PORT+".")
	WriteString GAME, LOGIN$ ;send the login
	
	Repeat
		If ReadAvail(GAME)
			id=ReadByte(GAME)
			Select id
				Case CONNECT_FAIL
					RuntimeError("Connected successfully, but denied by server.")
				Case CONNECT_SUCCESS
					Exit
			End Select
		End If
	Forever
End Function

Function readUpdates()
	If ReadAvail(GAME)
		id=ReadByte(GAME)
		Select id
			Case PLAYER_MOVE
				direction = ReadByte(GAME)
				move_id = ReadInt(GAME)
				move(move_id,direction)
				DebugLog "Somebody moving..."
			Case NEW_OBJ
				new_id=ReadByte(GAME)
				DebugLog "New spawn"
				Select new_id
					Case NEW_OBJ_MOB ;new mob
						M.mob=New mob
						M\name$=ReadString(GAME)
						M\x = ReadByte(GAME)
						M\y = ReadByte(GAME)
						M\ID = ReadInt(GAME)
						M\login$ = ReadString(GAME)
						M\dir = ReadByte(GAME)
						M\ALPHA = ReadByte(GAME)
						
						For i=0 To 8
							M\equip[i]=ReadInt(GAME)
						Next
						
						For i=0 To 5
							M\limbs[i]=ReadByte(GAME)
						Next
						
						For i=0 To 3
							M\damage[i]=ReadByte(GAME)
						Next
						DebugLog "New mob!"
					Case NEW_OBJ_OBJ
						x=ReadByte(GAME)
						y=ReadByte(GAME)
						BT=ReadByte(GAME)
						FR=ReadByte(GAME)
						ID=ReadInt(GAME)
						
						NEW_OBJ(BT,x,y,FR,ID)
						DebugLog "New object!"
					Case NEW_OBJ_ITEM
						x=ReadByte(GAME)
						y=ReadByte(GAME)
						b_type=ReadByte(GAME)
						id=ReadInt(GAME)
						
						NEW_ITEM(b_type,x,y,id)
					Default
						RuntimeError "Huh"
				End Select
			Case NEW_MSG
				msg$=ReadString(GAME)
				push_message(msg$)
			Case UPD_OBJ
				id=ReadByte(GAME) ;update id
				Select id
					Case UPD_HLTH
						id=ReadInt(GAME) ;object id
						O.obj=Object.obj(GET_OBJ(id))
						If O<>Null Then O\health=ReadInt(GAME)
					Case UPD_MOB
						id=ReadInt(GAME) ;mob ID
						M.mob=Object.mob(PlayerChar(id))
						If M=Null Then DebugLog"Error mob":Return
						For i=0 To 3
							M\damage[i]=ReadByte(GAME)
						Next
						stun=ReadByte(GAME)
						If M\stun=0 And stun>0 Then M\anim=1:DebugLog M\name$+" got stunned!"
						M\stun=stun
						M\move_delay=ReadInt(GAME)
						M\actn_delay=ReadInt(GAME)
				End Select
			Case UPD_ITEM
				id=ReadInt(GAME) ;item id
				DebugLog "Reading updates!"
				IT.item=Object.item(GET_ITEM(id))
				If IT<>Null
					IT\x=ReadByte(game)
					IT\y=ReadByte(game)
				EndIf
			Case DEL_OBJ
				id=ReadInt(GAME) ;object id
				O.obj=Object.obj(GET_OBJ(id))
				Delete O
			Case PLAYER_EQUIP
				loginQ$=ReadString(GAME)
				itemID=ReadInt(GAME)
				slot=ReadByte(GAME)
				M.mob=Object.mob(PlayerChar(0,loginQ$))
				;If M\EQUIP[slot]<>0
				;	IT.item=Object.item(GET_ITEM(M\EQUIP[slot]))
				;	If IT<>Null
				;		IT\x=M\x
				;		IT\y=M\y
				;		M\equip[slot]=0
				;	Else
				;		DebugLog "Possibly horrible error"
				;	EndIf
				;EndIf
				M\EQUIP[slot]=itemID
				DebugLog "Update: Slot "+slot+" is now "+itemID
		End Select
		Return 1
	End If
	Return 0
End Function

Function updateInput()
	If KeyHit(79);NUM1
		use_Item(SLOT_GLOVES)
	ElseIf KeyHit(80) ;NUM2
		use_Item(SLOT_JUMPSUIT)
	ElseIf KeyHit(81) ;NUM3
		use_Item(SLOT_BOOTS)
	ElseIf KeyHit(75) ;NUM4
		use_Item(SLOT_LEFTHAND)
	ElseIf KeyHit(76) ;NUM5
		use_Item(SLOT_EXOSUIT)
	ElseIf KeyHit(77) ;NUM6
		use_Item(SLOT_RIGHTHAND)
	ElseIf KeyHit(71) ;NUM7
		use_Item(SLOT_EAR)
	ElseIf KeyHit(72) ;NUM8
		use_Item(SLOT_HEAD)
	ElseIf KeyHit(73) ;NUM9
		use_Item(SLOT_MASK)
	EndIf

	If KeyHit(KEY_UP)
		svr_move(NORTH)
	ElseIf KeyHit(KEY_DOWN)
		svr_move(SOUTH)
	EndIf
	
	If KeyHit(KEY_LEFT)
		svr_move(WEST)
	ElseIf KeyHit(KEY_RIGHT)
		svr_move(EAST)
	EndIf
	
	If KeyHit(16)
		INTENT = Not INTENT
	EndIf
	
	If MouseHit(1)
		If MouseX()>256 And MouseX()<256+32 And MouseY()>512-64 And MouseY()<512-32
			INTENT=INTENT_HELP:Return
		EndIf
		If MouseX()>256 And MouseX()<256+32 And MouseY()>512-32 And MouseY()<512
			INTENT=INTENT_GRAB:Return
		EndIf
		If MouseX()>256+32 And MouseX()<256+64 And MouseY()>512-64 And MouseY()<512-32
			INTENT=INTENT_PUSH:Return
		EndIf
		If MouseX()>256+32 And MouseX()<256+64 And MouseY()>512-32 And MouseY()<512
			INTENT=INTENT_HARM:Return
		EndIf
		If MouseX()>512-64 And MouseX()<512 And MouseY()>512-96 And MouseY()<512
			For i=0 To 5
				If ImagesCollide(mouse_cursor,MouseX(),MouseY(),0,P_BAG(i,1),512-64,512-96,0)
					BODY_PART=i
					i=5
					Return
				EndIf
			Next
		EndIf
		
		mouseXPos = (MouseX()/32+camX)
		mouseYPos = (MouseY()/32+camY)
		
		If MouseXPos<0 Or mouseXPos>255 Or MouseYPos<0 Or MouseYPos>255 Then Return
		
		WriteByte GAME, PLAYER_CLICK
		WriteByte GAME, mouseXPos
		WriteByte GAME, mouseYPos
		WriteByte GAME, INTENT
	End If
End Function

Function use_Item(slot)
	WriteByte GAME, PLAYER_USE_ITEM
	WriteByte GAME, slot
End Function

Function move(id,dir)
	mob_H = PlayerChar(id)
	If mob_H = 0 Then Return
	M.mob=Object.mob(mob_H)
	camX=camX+D2X(dir)
	camY=camY+D2Y(dir)
	M\X=M\X+D2X(dir)
	M\Y=M\Y+D2Y(dir)
	M\dir=dir
End Function

Function svr_move(dir)
	WriteByte GAME, PLAYER_MOVE
	WriteByte GAME, dir
End Function

Function push_message(msg$)
	message_log(0)=message_log(1)
	message_log(1)=message_log(2)
	message_log(2)=message_log(3)
	message_log(3)=message_log(4)
	message_log(4)=msg$
End Function
