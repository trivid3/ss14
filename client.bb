Global OBJ_ID = 1
Global MAPEDIT= 1
SeedRnd MilliSecs()

Include "pregame.bb"
init_map()

For O.obj=Each obj:Delete O:Next
OBJ_ID = 1

Include "network_defines.bb"
Include "other_defines.bb"
Include "items.bb"

AppTitle "Space Station 14"

CONNECTION_IP$ = "127.0.0.1"
CONNECTION_PORT = 1234

Global LOGIN$ = "John"+Rand(0,999)
Global GAME
Global FIELD_OF_VISION=CreateBank(16*16)

Global INTENT, BODY_PART=0

connect(CONNECTION_IP$,CONNECTION_PORT)

Graphics3D 660,640,0,2
SetBuffer BackBuffer()

Dim message_log$(4)
Global font=LoadFont("Arial",12)
SetFont font

Include "FastImage.bb"
If Not InitDraw() Then RuntimeError("Uh oh")
Include "graphics.bb"

Global CAMX = 119 ; 128;128 is the spawn point
Global CAMY = 121

Global map_edit_choice=0
Global mouse_cursor=CreateImage(1,1) ;for clicking on images, sucks but w\e
SetBuffer ImageBuffer(mouse_cursor):Color 255,0,0:Rect 0,0,1,1:SetBuffer BackBuffer()

DebugLog map(128,128)\T_DARK#

DebugLog "All clear"

update_fov()

Repeat
	Cls
	StartDraw()
	renderScreen()
	renderGUI()
	editMap()
	readUpdates()
	updateInput()
	EndDraw()
	Flip
Forever

Function update_fov()
	M.Mob=Object.Mob(PlayerChar(0,LOGIN$))
	If M=Null Then DebugLog "Couldn't find player character!":Return
	For x=camX To camX+15
		For y=camY To camY+15
			If Not (x<0 Or x>255 Or y<0 Or y>255)
				PokeByte(FIELD_OF_VISION,(y-camY)*16+(x-camX),los(M\x,M\y,x,y))
			EndIf
		Next
	Next
End Function

Function editMap()
	If MouseHit(3)
		MouseTX = (MouseX()/32+camX)
		MouseTY = (MouseY()/32+camY)
		
		;map(MouseTX,MouseTY)\T_TYPE = map_edit_choice
		;map(MouseTX,MouseTY)\T_FRAME = 0
		NEW_OBJ(OBJ_LIGHT,MouseTX,MouseTY)
		;For O.obj=Each obj
		;	If O\x=MouseTX And O\y=MouseTY Then Delete O
		;Next
	EndIf
	
	If KeyHit(57) ;spacebar
		MAPEDIT=Not MAPEDIT
	EndIf
	
	If KeyHit(31) ;'S'
		saveMap()
	EndIf
End Function

Function saveMap()
	Local F = WriteFile("map/centcomm.ss")
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
		If MouseX()>512+2 And MouseX()<512+34 And MouseY()>450 And MouseY()<450+32
			INTENT=INTENT_HELP:Return
		EndIf
		If MouseX()>512+2 And MouseX()<512+34 And MouseY()>482 And MouseY()<482+32
			INTENT=INTENT_GRAB:Return
		EndIf
		If MouseX()>512+34 And MouseX()<512+66 And MouseY()>450 And MouseY()<450+32
			INTENT=INTENT_PUSH:Return
		EndIf
		If MouseX()>512+34 And MouseX()<512+66 And MouseY()>482 And MouseY()<482+32
			INTENT=INTENT_HARM:Return
		EndIf
		If MouseX()>512+5 And MouseX()<512+148 And MouseY()>220 And MouseY()<369
			For i=0 To 5
				If ImagesCollide(mouse_cursor,MouseX(),MouseY(),0,P_BAG(i,1),512+5,220,0)
					BODY_PART=i
					DebugLog i
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
	M.mob=Object.mob(mob_H)
	If M=Null Then RuntimeError "Error in move() function!!!"
	DebugLog M\name$+" moving..."
	M\X=M\X+D2X(dir)
	M\Y=M\Y+D2Y(dir)
	M\dir=dir
	
	If(LOGIN$=M\login$)
		camX=camX+D2X(dir)
		camY=camY+D2Y(dir)
		update_fov()
	EndIf
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