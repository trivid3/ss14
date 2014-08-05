Global OBJ_ID = 1

Include "pregame.bb"
init_map()

Include "network_defines.bb"
Include "other_defines.bb"
Include "items.bb"

AppTitle "Space Station 14 Server"

Global SERVER_PORT = 1234

Global SERVER = CreateTCPServer(SERVER_PORT)

Type TUser
	Field login$
	Field stream
End Type

Global worldtime

Repeat
	progressAll()
	
	newStream = AcceptTCPStream(SERVER)
	If newStream
		U.TUser = New TUser ;add a new user
		U\stream = newStream
		U\login$ = ReadString(U\stream)
		allow = 1
		Print DottedIP(TCPStreamIP(U\stream))
		If U\login$ = 0 Then allow=0
		If U\login$ = "" Then allow=0
		
		If allow
			WriteByte U\stream, CONNECT_SUCCESS ;tell him that connection is successful
			Print "A player ("+U\login$+") has connected successfully."
			NEW_MOB(U\login$, 126, 128)
			updateUser(U.TUser)
		Else
			WriteByte U\stream, CONNECT_FAIL
			Print "A player failed to connect."
		EndIf
	EndIf
	
	For U.TUser = Each TUser
		If ReadAvail(U\stream)
			id=ReadByte(U\stream)
			Select id
				Case PLAYER_MOVE
					dir=ReadByte(U\stream)
					allow=1
					M.mob=Object.mob(PlayerChar(0,U\login$))
					If M<>Null
						M\dir=dir
						If TILE_PASS(M\x+D2X(dir),M\y+D2Y(dir))=0 Then allow=0
					Else
						allow=0
					EndIf
					If allow
						For TU.TUser=Each TUser
							WriteByte U\stream, PLAYER_MOVE
							WriteByte U\stream, dir
							WriteInt U\stream, M\ID
							M\X=M\X+D2X(dir)
							M\Y=M\Y+D2Y(dir)
							Print "Mob ("+M\login$+") moved to "+M\X+";"+M\Y
						Next
					EndIf
				Case PLAYER_CLICK
					x=ReadByte(U\stream)
					y=ReadByte(U\stream)
					intent=ReadByte(U\stream)
					
					handle_obj_click(x,y,U\login$,intent)
				Case PLAYER_USE_ITEM
					PSlot=ReadByte(U\stream)
					PChar=PlayerChar(0,U\login$)
					use_item(PSlot,PChar)
				Default
					Print "Unknown packet ID ("+id+")"
			End Select
		Else ;assume the worst
			Select Eof(U\stream)
				Case -1 ;unexpected
					Print U\login$+" has crashed!"
					endLife(U.TUser)
				Case 1 ;disconnect
					Print U\login$+" has disconnected."
					endLife(U.TUser)
			End Select
		EndIf
	Next
	If KeyHit(1) Exit
Forever
End

Function progressAll()
	For M.mob=Each mob
		If worldtime Mod 100 = 0
			Life(M.mob)
		EndIf
	Next
	worldtime=worldtime+1
End Function

Function Life(M.mob)
	Local updated=0
	If M\stun>0
		M\stun=M\stun-1
		If M\stun=0 Then MOB_Update_Stats(M.mob):Print "Stun at 0"
	EndIf
	If M\actn_delay>0
		M\actn_delay=M\actn_delay-1
		If M\actn_delay=0 Then MOB_Update_Stats(M.mob):Print "Action delay at 0"
	EndIf
	If M\move_delay>0
		M\move_delay=M\move_delay-1
		If M\move_delay=0 Then MOB_Update_Stats(M.mob):Print "Move delay at 0"
	EndIf
End Function

Function endLife(U.TUser)
	M.mob=Object.mob(playerChar(0,U\login$))
	If M<>Null Then M\login$=""
	Delete U
End Function

Function updateUser(U.TUser)
	;First all the items
	For I.item=Each item
		ITEM_Send_Info(I.item, U.TUser)
	Next
	
	;Then objects
	For O.obj=Each obj
		OBJ_Send_Info(O.obj, U.TUser)
	Next
	
	;Then finally mobs
	For M.mob=Each mob
		If M\login$<>U\login$ ;Don't need to create another copy of the user
			MOB_Send_Info(M.mob, U.TUser)
		EndIf
	Next
	
End Function

Function NEW_MOB(login$,x,y)
	M.mob=New mob
	M\name$=generate_random_name()
	M\login$=login$
	M\x=x
	M\y=y
	M\ID=MOB_ID
	MOB_ID=MOB_ID+1
	For U.TUser = Each TUser
		MOB_Send_Info(M.mob, U.TUser)
	Next
End Function

Function MOB_Send_Info(M.mob, U.TUser)
	If M=Null Then Return
	If U=Null Then Return
	WriteByte U\stream, NEW_OBJ
	WriteByte U\stream, NEW_OBJ_MOB
	WriteString U\stream, M\name$
	WriteByte U\stream, M\x
	WriteByte U\stream, M\y
	WriteInt U\stream, M\ID
	WriteString U\stream, M\login$
	WriteByte U\stream, M\dir
	
	;color shit
	WriteByte U\stream, 25
	
	;equipment
	For i=0 To 8
		WriteInt U\stream, M\equip[i]
		Print "Sent "+M\equip[i]
	Next
	
	For i=0 To 5
		WriteByte U\stream, M\limbs[i]
	Next
	
	For i=0 To 3
		WriteByte U\stream, M\damage[i]
	Next
End Function

Function OBJ_Send_Info(O.obj, U.TUser)
	If O=Null Then Return
	If U=Null Then Return
	WriteByte U\stream, NEW_OBJ
	WriteByte U\stream, NEW_OBJ_OBJ ;d
	WriteByte U\stream, O\x
	WriteByte U\stream, O\y
	WriteByte U\stream, O\B_TYPE
	WriteByte U\stream, O\B_FRAME
	WriteInt U\stream, O\ID
End Function

Function ITEM_Send_Info(I.item, U.TUser)
	If I=Null Then Return
	If U=Null Then Return
	WriteByte U\stream, NEW_OBJ
	WriteByte U\stream, NEW_OBJ_ITEM
	WriteByte U\stream, I\x
	WriteByte U\stream, I\y
	WriteByte U\stream, I\b_type
	WriteInt U\stream, I\ID
End Function

Function handle_obj_click(x,y,login$,intent)
	U.TUser=Object.TUser(PlayerTUser(login$))
	MU.Mob=Object.mob(PlayerChar(0,login$))
	If MU=Null Then Print "Invalid mob ("+login$+")!":Return
	If U=Null Then Print "Invalid login ("+login$+")!":Return
	For O.obj=Each obj
		If O\x=x And O\y=y
			Select O\B_TYPE
				Case OBJ_GRILLE
					If(intent = INTENT_HARM)
						Print O\health
						If O\health>20
							push_message(U.TUser, "You kick the grille!")
							O\health=O\health-20
							update_obj(O.obj,UPD_HLTH)
							Print "Kicked a grille "+O\ID
							Return
						Else
							push_message(U.TUser, "You destroy the grille!")
							O\health=0
							Print "Removed a grille "+O\ID
							remove_obj(O.obj)
							Return
						EndIf
					EndIf
				Case OBJ_WINDOW
					Return
				Default
					Print "UNKNOWN OBJECT"
					Return
			End Select
		EndIf
	Next
	
	For M.Mob=Each Mob
		If M\x=x And M\y=y
			TU.TUser=Object.TUser(M\login$)
			Select intent
				Case INTENT_HELP
					push_message(U.TUser, "You hug "+M\name$+".")
					visible_message(U.TUser, MU\name$+" hugs "+M\name$+".")
				Case INTENT_PUSH ;disarm
					outcome=Rand(0,4)
					If outcome=0
						push_message(U.TUser, "You disarm "+M\name$+".")
						visible_message(U.TUser, MU\name$+" disarms "+M\name$+".")
					ElseIf outcome=1
						push_message(U.TUser, "You disarm "+M\name$+" and push him down!")
						visible_message(U.TUser, MU\name$+" pushes "+M\name$+" down!")
						mob_stun(M.mob,4000)
					Else
						push_message(U.TUser, "You try to disarm "+M\name$+".")
						visible_message(U.TUser, MU\name$+" tries to disarm "+M\name$+".")
					EndIf
				Case INTENT_HARM ;the robust comes
			End Select
		EndIf
	Next
	
	For I.Item=Each Item ;if player clicked on an item
		If I\x=x And I\y=y
			writing_slot=255
			If MU\Equip[SLOT_LEFTHAND]=0 ;left hand is empty
				writing_slot=SLOT_LEFTHAND ;put it in the left hand
			ElseIf MU\EQUIP[SLOT_RIGHTHAND]=0 ;right hand is empty
				writing_slot=SLOT_RIGHTHAND ;put it in the right hand
			Else
				Return
			EndIf
			I\x=0
			I\y=0
			MOB_Update_Equip(U\login$,I\ID,writing_slot)
			MU\EQUIP[writing_slot]=I\ID
			
			ITEM_Send_Update(I.Item)
		EndIf
	Next
End Function

Function mob_stun(M.mob,duration=5)
	If M=Null Then Return
	;drop all items
	If M\Equip[SLOT_LEFTHAND]<>0
		IT.Item=Object.Item(GET_ITEM(M\EQUIP[SLOT_LEFTHAND]))
		IT\X=M\X
		IT\Y=M\Y
		M\EQUIP[SLOT_LEFTHAND]=0
		MOB_Update_Equip(M\login$,0,SLOT_LEFTHAND)
		ITEM_Send_Update(IT.Item)
	EndIf
	If M\Equip[SLOT_RIGHTHAND]<>0
		IT.Item=Object.Item(GET_ITEM(M\EQUIP[SLOT_RIGHTHAND]))
		IT\X=M\X
		IT\Y=M\Y
		M\EQUIP[SLOT_RIGHTHAND]=0
		MOB_Update_Equip(M\login$,0,SLOT_RIGHTHAND)
		ITEM_Send_Update(IT.Item)
	EndIf
	;then stun the fag
	If M\stun>0
		duration=duration*0.5
	EndIf
	M\stun=duration
	M\move_delay=duration+10
	M\actn_delay=duration+12
	MOB_Update_Stats(M.mob)
	Print "Done updating crap"
End Function

Function push_message(U.TUser,msg$)
	If U=Null Then Return 0
	WriteByte U\stream, NEW_MSG
	WriteString U\stream, msg$
	DebugLog msg$
End Function

Function ITEM_Send_Update(I.Item)
	For U.TUser=Each TUser
		WriteByte U\stream, UPD_ITEM
		WriteInt U\stream, I\ID
		WriteByte U\stream, I\x
		WriteByte U\stream, I\y
	Next
End Function

Function MOB_Update_Equip(login$,id,slot)
	For U.TUser=Each TUser
		WriteByte U\stream, PLAYER_EQUIP
		WriteString U\stream, login$
		WriteInt U\stream, id
		WriteByte U\stream, slot
	Next
End Function

Function MOB_Update_Stats(M.mob)
	If M=Null Then Print"Invalid mob called in MOB_Update_Stats":Return
	
	For U.TUser=Each TUser
		WriteByte U\stream, UPD_OBJ
		WriteByte U\stream, UPD_MOB
		WriteInt U\stream, M\ID
		For i=0 To 3:WriteByte U\stream, M\DAMAGE[i]:Next ;only damage for now
		WriteByte U\stream, M\stun
		WriteInt U\stream, M\move_delay
		WriteInt U\stream, M\actn_delay
		Print "Sent an update for "+U\LOGIN$
	Next
End Function

Function visible_message(U.TUser,msg$) ;sends message to everyone but U.TUser
	If U=Null Then Return 0
	For TU.TUser=Each TUser
		If U<>TU And TU\login$<>0 Then push_message(TU.TUser,msg$)
	Next
End Function

Function PlayerTUser(login$)
	For U.TUser=Each TUser
		If U\login$=login$ Then Return Handle(U)
	Next
	Return 0
End Function

Function remove_obj(O.obj)
	For U.TUser=Each TUser
		WriteByte U\stream, DEL_OBJ
		WriteInt U\stream, O\ID
	Next
	Delete O
End Function

Function update_obj(O.obj,upd_type)
	For U.TUser=Each TUser
		WriteByte U\stream, UPD_OBJ
		WriteByte U\stream, upd_type
		WriteInt U\stream, O\ID
		Select upd_type
			Case UPD_HLTH
				WriteInt U\stream, O\health
		End Select
	Next
End Function

Function use_item(slot,mobHandle)
	M.mob=Object.mob(mobHandle):If M=Null Then Print"No mob!":Return
	U.TUser=Object.TUser(PlayerTUser(M\login$)): If U=Null Then Print"No user!":Return
	IT.item=Object.item(get_item(M\equip[slot])): If IT=Null Then Print"No item!":Return
	
	Print "Using item ("+U\login$+"; "+IT\name$+")"
	
	If IT\I_TYPE=ITEM_CLOTHING Or IT\I_TYPE=ITEM_MASK
		Local new_slot
		Select IT\I_TYPE
			Case ITEM_CLOTHING
				new_slot=SLOT_JUMPSUIT
			Case ITEM_MASK
				new_slot=SLOT_FACE
		End Select
		If slot=SLOT_LEFTHAND Or slot=SLOT_RIGHTHAND ;put it on
			If M\equip[new_slot]=0
				M\equip[new_slot]=M\equip[slot] ;move item from hand onto the character
				M\equip[slot]=0
				MOB_Update_equip(M\login$,0,slot)
				MOB_Update_equip(M\login$,IT\ID,new_slot)
			EndIf
		ElseIf slot=new_slot ;take it off
			IT\X=M\X
			IT\Y=M\Y
			M\equip[new_slot]=0
			ITEM_Send_Update(IT.item)
			MOB_Update_equip(M\login$,0,new_slot)
		EndIf
	EndIf
	
	Select IT\B_TYPE
		Case ITEM_BIKE_HORN
			push_message(U.TUser,"You honk!")
			visible_message(U.TUser,"HONK")
	End Select
End Function