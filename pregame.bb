Type TILE
	Field T_TYPE
	Field T_FRAME
	Field T_DARK#
	Field T_LR,T_LG,T_LB
End Type

Dim map.TILE(255,255)

Function init_map()
	F = ReadFile("map/station.ss")
	If Not F Then RuntimeError("Map not found!")
	
	For x=0 To 255
		For y=0 To 255
			map.TILE(x,y) = New TILE
			map(x,y)\T_TYPE = ReadByte(F)
			map(x,y)\T_FRAME = ReadByte(F)
			If map(x,y)\T_TYPE = TILE_SPACE
				map(x,y)\T_FRAME = Rand(0,7)
			EndIf
			map(x,y)\T_DARK# = 0
		Next
	Next
	
	Amount = ReadInt(F) ;count of OBJ
	If Amount<>0
		For i=0 To Amount-1
			x=ReadByte(F)
			y=ReadByte(F)
			b_t=ReadByte(F)
			b_f=ReadByte(F)
			NEW_OBJ(b_t,x,y,b_f)
		Next
	EndIf
	
	Print "Map initialized"

	CloseFile F
End Function

Function update_lights()
	For T.TILE=Each TILE
		T\T_DARK# = 1
		If T\T_TYPE=TILE_SPACE Then T\T_DARK#=0
	Next
	For O.obj=Each obj
		If O\luminosity>0
			map(O\x,O\y)\T_DARK=0
			max=O\luminosity
			bank=CreateBank(1)
			
			For a=0 To 359 Step 4
				For i=1 To max+4
					dark#=map(O\x+Cos(a)*i,O\y+Sin(a)*i)\T_DARK#
					newDark#=(Float(i)/Float(max+4))
					
					If newDark#<dark#
						map(O\x+Cos(a)*i,O\y+Sin(a)*i)\T_DARK#=newDark#
					EndIf
					If Not TILE_TRANSP(O\x+Cos(a)*i,O\y+Sin(a)*i) Then i=max+4
				Next
			Next
			
			FreeBank bank
		EndIf
	Next
	DebugLog "Updating lights..."
End Function

Function max(v1,v2)
	If v2>v1 Return v2 Else Return v1
End Function

Function min(v1#,v2#)
	If v2<v1 Return v2 Else Return v1
End Function