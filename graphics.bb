Dim walls(7)
walls(0)=LoadImg("graphics/tiles/wall.png",1)
walls(1)=LoadImg("graphics/tiles/wall1.png",1)
walls(2)=LoadImg("graphics/tiles/wall2.png",1)

Global space_yoba = LoadImg("graphics/tiles/space_bg.png", 1)
Global space_yoba_x# = 0
Dim space(7)
For i=0 To 7
	space(i)=LoadImg("graphics/tiles/space"+i+".png")
Next

Dim floors(7)
floors(0)=LoadImg("graphics/tiles/floor.png",1)
floors(1)=LoadImg("graphics/tiles/floor1.png",1)
floors(2)=LoadImg("graphics/tiles/floor2.png",1)

Dim mobs(7)
mobs(0)=LoadAnimImg("graphics/mobs/human.png",32,32,0,4)

Dim structures(7)
structures(0)=LoadImg("graphics/objects/grille.png"): Const IMG_GRILLE = 0
structures(1)=LoadImg("graphics/objects/window.png"): Const IMG_WINDOW = 1
structures(2)=LoadImg("graphics/objects/light.png"): Const IMG_LIGHT = 2

Dim GUI(7)
GUI(0)=LoadImage("graphics/gui/left.png")
GUI(1)=LoadImg("graphics/gui/chat.png",1)
GUI(2)=LoadImg("graphics/gui/intents_full.png",1)
GUI(3)=LoadImg("graphics/gui/punching_bag/punching_bag.png",1)
GUI(4)=LoadImg("graphics/gui/inv_slot.png",1)
GUI(5)=LoadImg("graphics/gui/select.png")

Dim P_BAG(5,1) ;6
P_BAG(0,0)=LoadImg("graphics/gui/punching_bag/head.png")
P_BAG(1,0)=LoadImg("graphics/gui/punching_bag/torso.png")
P_BAG(2,0)=LoadImg("graphics/gui/punching_bag/l_arm.png")
P_BAG(3,0)=LoadImg("graphics/gui/punching_bag/r_arm.png")
P_BAG(4,0)=LoadImg("graphics/gui/punching_bag/l_leg.png")
P_BAG(5,0)=LoadImg("graphics/gui/punching_bag/r_leg.png")
P_BAG(0,1)=LoadImage("graphics/gui/punching_bag/head.png"):MaskImage P_BAG(0,1),255,255,255
P_BAG(1,1)=LoadImage("graphics/gui/punching_bag/torso.png"):MaskImage P_BAG(1,1),255,255,255
P_BAG(2,1)=LoadImage("graphics/gui/punching_bag/l_arm.png"):MaskImage P_BAG(2,1),255,255,255
P_BAG(3,1)=LoadImage("graphics/gui/punching_bag/r_arm.png"):MaskImage P_BAG(3,1),255,255,255
P_BAG(4,1)=LoadImage("graphics/gui/punching_bag/l_leg.png"):MaskImage P_BAG(4,1),255,255,255
P_BAG(5,1)=LoadImage("graphics/gui/punching_bag/r_leg.png"):MaskImage P_BAG(5,1),255,255,255

Dim ITEMS(31)
ITEMS(ITEM_JUMPSUIT)=LoadAnimImg("graphics/items/clothing/jumpsuit.png",32,32,0,5)
ITEMS(ITEM_JUMPSUIT_1)=LoadAnimImg("graphics/items/clothing/jumpsuit1.png",32,32,0,5)
ITEMS(ITEM_CLOWN_MASK)=LoadAnimImg("graphics/items/clothing/clown_mask.png",32,32,0,5)
ITEMS(ITEM_BIKE_HORN)=LoadAnimImg("graphics/items/honk.png",32,32,0,5)

Dim OVERLAYS(7)
OVERLAYS(0)=LoadImg("graphics/effects/blood.png")

AutoMidHandleEx(0)

Function LoadImg(adress$, flags=3)
	img=LoadImageEx(adress$,flags)
	SetImageHandle img, 0, 0
	If Not img Then img=CreateImage(64,64)
	Return img
End Function

Function LoadAnimImg(adress$, frameW, frameH, frameF, frameC, flags=3)
	img=LoadAnimImageEx(adress$, flags, frameW, frameH, frameF, frameC, 1)
	SetImageHandle img, 0, 0
	If Not img Then img=CreateImageEx(frameW, frameH, frameC)
	Return img
End Function

Function drawTile(tile_type, x, y, tile_frame = 0)
	Select TILE_TYPE
		Case T_SPACE
			DrawImageEx space(tile_frame), x, y
		Case T_FLOOR
			DrawImageEx floors(tile_frame), x, y
		Case T_WALL
			DrawImageEx walls(tile_frame), x, y
	End Select
End Function

Function drawObj(O.obj, x, y)
	If O=Null Then Return
	x=x+O\off_x
	y=y+O\off_y
	Select O\B_TYPE
		Case OBJ_GRILLE
			DrawImageEx structures(IMG_GRILLE), x, y
		Case OBJ_WINDOW
			DrawImageEx structures(IMG_WINDOW), x, y
		Case OBJ_LIGHT
			SetOrigin(0,0)
			SetRotation(D2A(O\dir))
			DrawImageEx structures(IMG_LIGHT), x, y
	End Select
End Function

Function renderScreen()
	space_yoba_x#=space_yoba_x#+0.1
	If space_yoba_x#>=128 Then space_yoba_x#=0
	For x=-1 To 4
		For y=0 To 4
			DrawImageEx space_yoba,x*128+space_yoba_x,y*128
		Next
	Next
	drawMap()
	drawObjects()
	drawItems()
	drawMobs()
	
	drawLight()
	
	SetColor 50,60,100
	DrawRect 512,0,148,GraphicsHeight()
	DrawRect 0,512,512,128
	SetColor 255,255,255
End Function

Function renderGUI()
	M.Mob=Object.Mob(PlayerChar(0,LOGIN$)) ;get your mob
	If M=Null Then Return
	DrawImageEx GUI(1), 0,GraphicsHeight()-24 ;chatbox
	DrawImage GUI(0), 512,0 ;whole left panel
	
	If M\EQUIP[SLOT_LEFTHAND]<>0
		IN.Item=Object.Item(GET_ITEM(M\EQUIP[SLOT_LEFTHAND])) ;DRAWING A LOT OF SHIT
		DrawImageEx items(IN\B_TYPE), 512-128-64,512-64 ;VERY LONG
	EndIf ;SORRY
	If M\EQUIP[SLOT_RIGHTHAND]<>0
		IN.Item=Object.Item(GET_ITEM(M\EQUIP[SLOT_RIGHTHAND]))
		DrawImageEx items(IN\B_TYPE), 512-128, 512-64
	EndIf
	If M\EQUIP[SLOT_JUMPSUIT]<>0
		IN.Item=Object.Item(GET_ITEM(M\EQUIP[SLOT_JUMPSUIT]))
		DrawImageEx items(IN\B_TYPE), 512-128-32, 512-32
	EndIf
	If M\EQUIP[SLOT_FACE]<>0
		IN.Item=Object.Item(GET_ITEM(M\EQUIP[SLOT_FACE]))
		DrawImageEx items(IN\B_TYPE), 512-128, 512-96
	EndIf
	;draw inventory
	SetAlpha(0.5)
	Select INTENT ;intentbox intents
		Case INTENT_HELP
			SetColor 20,150,20
			DrawRect 512+2,450,28,28,1
		Case INTENT_PUSH
			SetColor 20,20,150
			DrawRect 512+34,450,28,28,1
		Case INTENT_GRAB
			SetColor 150,150,20
			DrawRect 512+2,482,28,28,1
		Case INTENT_HARM
			SetColor 150,20,20
			DrawRect 512+34,482,28,28,1
	End Select
	SetColor 255,255,255
	SetAlpha(1)
	DrawImageEx GUI(3), 512+5, 220 ;punching bag
	SetAlpha(0.4)
	DrawImageEx P_BAG(BODY_PART,0),512+5,220 ;selected body part
	SetAlpha(1)
	
	For i=0 To 4
		Text 24, 512 - 64 - 12 + 4 + i * 12, message_log$(i)
	Next
End Function

Function drawMap()
	For x=camX To camX+15
		For y=camY To camY+15
			If Not (x<0 Or x>255 Or y<0 Or y>255)
				If PeekByte(FIELD_OF_VISION,(y-camY)*16+(x-camX))=1
					drawTile(map(x,y)\T_Type, (x-camX)*32, (y-camY)*32, map(x,y)\T_FRAME)
				Else
					SetColor 0,0,5
					DrawRect (x-camX)*32, (y-camY)*32, 32, 32
					SetColor 255,255,255
				EndIf
			EndIf
		Next
	Next
End Function



Function drawLight()
	If MAPEDIT Then Return
	For x=camX To camX+15
		For y=camY To camY+15
			If Not(x<0 Or x>255 Or y<0 Or y>255)
				SetAlpha(map(x,y)\T_DARK#)
				SetColor 0,0,0
				If PeekByte(FIELD_OF_VISION,(y-camY)*16+(x-camX))=1
					DrawRect((x-camX)*32,(y-camY)*32,32,32,1)
				EndIf
				
				SetAlpha(1)
				SetColor 255,255,255
			EndIf
		Next
	Next
End Function

Function drawItems()
	For I.item = Each item
		If I\x>=camX And I\x<=camX+15 And I\y>=camY And I\y<=camY+15
			If PeekByte(FIELD_OF_VISION,(I\y-camY)*16+(I\x-camX))=1
				DrawImageEx items(I\B_TYPE), (I\x-camX)*32, (I\y-camY)*32, 0
				Text (I\x-camX)*32,(I\y-camY)*32+24,I\name$
			EndIf
		EndIf
	Next
End Function

Function drawMobs()
	For M.mob = Each mob
		If M\x>=camX And M\x<=camX+15 And M\y>=camY And M\y<=camY+15
			If PeekByte(FIELD_OF_VISION,(M\y-camY)*16+(M\x-camX))=1
				SetAlpha(M\ALPHA)
				If M\stun>0
					If M\anim>0
						SetRotation(M\anim*10):SetOrigin(Float(M\anim)/9.0*32.0,0)
						M\anim=M\anim+1
						If M\anim>9 Then M\anim=9
					EndIf
				EndIf
				DrawImageEx mobs(0), (M\x-camX)*32, (M\y-camY)*32, M\dir
				For i=0 To 3
					goodslot=0
					If i=SLOT_FACE Or i=SLOT_JUMPSUIT Then goodslot=1
					If M\EQUIP[i]<>0
						IT.ITEM=Object.ITEM(GET_ITEM(M\EQUIP[i]))
						If isWearable(IT.Item) And  goodslot ;if wearing clothing
							DrawImageEx items(IT\B_TYPE), (M\x-camX)*32, (M\y-camY)*32, M\dir+1
						EndIf
					EndIf
					SetRotation(0)
					SetOrigin(0,0)
				Next
				Text (M\x-camX)*32,(M\y-camY)*32+30, M\name$
				SetAlpha(1)
			EndIf
		EndIf
	Next
End Function

Function drawObjects()
	For O.obj = Each obj
		If O\x>=camX And O\x<=camX+15 And O\y>=camY And O\y<=camY+15
			If PeekByte(FIELD_OF_VISION,(O\y-camY)*16+(O\x-camX))=1
				DrawObj(O.obj, (O\x-camX)*32, (O\y-camY)*32)
			EndIf
		EndIf
	Next
End Function