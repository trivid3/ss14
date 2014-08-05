;Directions
Const SOUTH = 0
Const EAST = 1
Const NORTH = 2
Const WEST = 3

;Tiles
Const T_SPACE = 0
Const T_WALL = 1
Const T_FLOOR = 2

;Keys
Const KEY_UP = 200
Const KEY_DOWN = 208
Const KEY_RIGHT = 205
Const KEY_LEFT = 203

Const INTENT_HELP=0
Const INTENT_HARM=1
Const INTENT_GRAB=2
Const INTENT_PUSH=3

Const BODY_HEAD=0
Const BODY_TORSO=1
Const BODY_LARM=2
Const BODY_RARM=3
Const BODY_LLEG=4
Const BODY_RLEG=5

Const SLOT_LEFTHAND=0
Const SLOT_RIGHTHAND=1
Const SLOT_JUMPSUIT=2
Const SLOT_FACE=3
Const SLOT_FEET=4
Const SLOT_EXOSUIT=5

Const DMG_BRUTE=0
Const DMG_BURN=1
Const DMG_TOX=2
Const DMG_OXY=3

Function LoS(x1,y1,x2,y2) ;wir waren namen LoS
	Local a=ATan2(y1-y2,x1-x2)
	Local dx#=-Cos(a)
	Local dy#=-Sin(a)
	
	Local nx#=x1
	Local ny#=y1
	
	Repeat
		If dist(Int(nx#),Int(ny#),x2,y2)<=1 Then Return 1
		
		nx#=nx#+dx#
		ny#=ny#+dy#
				
		If nx#<0 Or nx#>255 Or ny#<0 Or ny#>255
			Return 0
		EndIf
		
		If TILE_TRANSP(Int(nx),Int(ny))=0
			Return 0
		EndIf
	Forever
	
End Function

Function dist(x1,y1,x2,y2)
	Return Sqr((x2-x1)^2+(y2-y1)^2)
End Function

Type MOB
	Field login$,name$
	Field x, y, dir
	Field ID
	
	;color shit
	Field ALPHA ;transparency
	Field EQUIP[8]
	Field LIMBS[5]
	Field DAMAGE[3]
	Field MOVE_DELAY
	Field ACTN_DELAY
	
	Field stun
	Field anim ;temporary variable for animating falls etc
End Type
Global MOB_ID = 1

Function PlayerChar(id=0, login$="")
	For M.mob=Each mob
		If (id=0 And M\login$=LOGIN$) Or (M\id = id) Or (login$<>"" And M\login$ = login$);if ID is 0, find the player
			Return Handle(M)
		EndIf
	Next
	Return 0
End Function

Function D2X(dir) ;dir to x
	Select dir
		Case EAST
			Return 1
		Case WEST
			Return -1
		Default
			Return 0
	End Select
End Function

Function D2Y(dir) ;dir to y
	Select dir
		Case NORTH
			Return -1
		Case SOUTH
			Return 1
		Default
			Return 0
	End Select
End Function

Function D2A(dir) ;dir to angle
	Select dir
		Case NORTH
			Return 0
		Case EAST
			Return 90
		Case SOUTH
			Return 180
		Case WEST
			Return 270
	End Select
End Function

Function TILE_PASS(x,y)
	If map(x,y)\T_TYPE = T_WALL
		Return 0
	EndIf
	
	For O.obj=Each obj
		If O\density=1
			If O\x=x And O\y=y
				Return 0
			EndIf
		EndIf
	Next
	
	Return 1
End Function

Function TILE_TRANSP(x,y)
	If map(x,y)\T_TYPE = T_WALL
		Return 0
	EndIf
	
	For O.obj=Each obj
		If O\transparent=0
			If O\x=x And O\y=y
				Return 0
			EndIf
		EndIf
	Next	
	
	Return 1
End Function

Type OBJ
	Field B_TYPE
	Field B_FRAME
	
	Field x,y
	Field off_x,off_y
	Field health
	Field name$
	Field desc$
	Field ID
	
	Field luminosity
	Field density
	Field transparent
	
	Field dir
End Type
OBJ_ID = 1

Function NEW_OBJ(B_TYPE,x,y,B_FRAME=0,ID=0)
	O.obj=New obj
	O\x=x
	O\y=y
	O\B_TYPE = B_TYPE
	O\B_FRAME = B_FRAME
	
	Select B_TYPE
		Case OBJ_GRILLE
			O\health = 60
			O\name$ = "grille"
			O\desc$ = "A construction built from metal rods"
			O\density = 1
			O\transparent = 1
		Case OBJ_WINDOW
			O\health = 150
			O\name$ = "window"
			O\desc$ = "Extremely thick and reinforced plate of glass which allows one to observe the beauty of things behind it."
			O\density = 1
			O\transparent = 1
		Case OBJ_LIGHT
			O\health = 5
			O\name$ = "light"
			O\desc$ = "A light source"
			O\density = 0
			O\transparent = 1
			O\luminosity = 10
			O\dir=NORTH
			O\off_x=0
			O\off_y=4
	End Select
	
	If ID=0
		O\ID = OBJ_ID
		OBJ_ID=OBJ_ID+1
	Else
		O\ID = ID
	EndIf
	
	update_lights()
End Function

Function GET_OBJ(id)
	For O.obj=Each obj
		If O\id=id Then Return Handle(O)
	Next
	Return 0
End Function

;objects
Const OBJ_GRILLE = 0

Const OBJ_WINDOW = 1

Const OBJ_LIGHT = 2

Dim array$(100)

Function rndText$(textInput$) ;"ABBA|BAAB|AAAA|BBBB" will return ABBA, BAAB, AAAA or BBBB with equal chance, | divides text
	Local numberOfPossibleStrings=1
	If Right(textInput$,1)="|" Then numberOfPossibleStrings=0
	Local i,x,c=1
	Local strForReturn$
	
	For x=1 To Len(textInput$)
		If Mid(textInput$,x,1)="|"
			numberOfPossibleStrings=numberOfPossibleStrings+1
		EndIf
	Next
	
	For x=1 To numberOfPossibleStrings
		Repeat
			i=i+1
			If Mid(textInput$,i,1)<>"|"
				array$(x)=array$(x)+Mid(textInput$,i,1)
			EndIf
		Until i>Len(textInput$) Or Mid(textInput$,i,1)="|"
	Next
	
	Return array$(Rand(1,numberOfPossibleStrings))
End Function

Const WRAP_MODE_NONE = 0
Const WRAP_MODE_WRAPPED = 1
Const WRAP_MODE_DIRECT = 2
Const ALIGN_MODE_LEFT = 0
Const ALIGN_MODE_CENTER = 1
Const ALIGN_MODE_RIGHT = 2
Const JUSTIFIED_OFF = 0
Const JUSTIFIED_ON = 1
Function WrapText%(x, y, txt$, mode = WRAP_MODE_NONE, align = ALIGN_MODE_LEFT, width = -1, justified = JUSTIFIED_OFF, height = -1) ;CREDIT TO http://gosse.proboards.com/index.cgi?board=programming&action=display&thread=136
    Local i, l, o, h
    Local w#, wx#
    Local c$
    l = Len(txt)
    If width < 0 And mode > WRAP_MODE_NONE Then
        RuntimeError "Cannot have no width with a wrap mode"
    End If
    If width < 0 And justified > JUSTIFIED_OFF Then
        RuntimeError "Cannot have no width and be justified"
    End If
    If height = -1 Then
        height = StringHeight(txt)
    End If
    Select mode
        Case WRAP_MODE_NONE
            For i = 1 To l
                If Mid(txt, i, 1) = Chr$(13) Or Mid(txt, i, 1) = Chr$(10) Then
                    h = WrapText(x, y, Left(txt, i - 1), WRAP_MODE_DIRECT, align, width, JUSTIFIED_OFF, height)
                    Return h+WrapText(x, y+height, Mid(txt, i + 1), mode, align, width, justified, height)
                End If
            Next
            Return WrapText(x, y, txt, WRAP_MODE_DIRECT, align, width, JUSTIFIED_OFF, height)
        Case WRAP_MODE_WRAPPED
            For i = 1 To l
                If Mid(txt, i, 1) = Chr$(13) Or Mid(txt, i, 1) = Chr$(10) Then
                    h = WrapText(x, y, Left(txt, i - 1), WRAP_MODE_DIRECT, align, width, JUSTIFIED_OFF, height)
                    Return h+WrapText(x, y+height, Mid(txt, i + 1), mode, align, width, justified, height)                
                End If
                If StringWidth(Left(txt, i - 1)) > width Then
                    i = i - 1
                    While i > 0 And Mid(txt, i, 1) <> " "
                        i = i - 1
                    Wend
                    If i = 0 Then
                        i = 1
                    End If
                    h = WrapText(x, y, Left(txt, i - 1), WRAP_MODE_DIRECT, align, width, justified, height)
                    Return h+WrapText(x, y+height, Mid(txt, i + 1), mode, align, width, justified, height)    
                End If
            Next
            Return WrapText(x, y, txt, WRAP_MODE_DIRECT, align, width, JUSTIFIED_OFF, height)
        Case WRAP_MODE_DIRECT
            Select justified
                Case JUSTIFIED_OFF
                    Select align
                        Case ALIGN_MODE_LEFT
                            Text x, y, txt
                        Case ALIGN_MODE_CENTER
                            Text x + width/2 - StringWidth(txt)/2, y, txt
                        Case ALIGN_MODE_RIGHT
                            Text x + width - StringWidth(txt), y, txt
                    End Select        
				Case JUSTIFIED_ON
                    w = (Float(width) - Float(StringWidth(txt))) / Float(l-1)
                    wx = x
                    For i = 1 To l
                        c$ = Mid(txt, i, 1)
                        Text wx, y, c
                        wx = wx + w + StringWidth(c)
                    Next
            End Select
            Return height
    End Select
End Function

Function isWearable(IT.ITEM)
	If IT\I_TYPE=ITEM_CLOTHING Or IT\I_TYPE=ITEM_MASK Then Return 1
	Return 0
End Function

Function generate_random_name$()
	Local firstnames$="John|Jack|Jose|Jesus|Jan|Julius|Joshua"
	Local secondnames$="Black|White|Red|Gray"
	Return rndText$(firstnames$+" "+secondnames$)
End Function