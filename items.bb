Const LOC_GROUND=0
Const LOC_PLAYER=1

Const ITEM_ACTIVABLE=0

Const ITEM_CLOTHING=1

Const ITEM_MASK=2
;Clothing items need 5 framed sprites
;One for each direction of a character + inventory sprite

Type ITEM
	Field x,y
	Field id
	Field b_type ;base type like 'bike horn' or 'jumpsuit'
	Field i_type ;item type like 'activable' or 'clothing'
	
	Field name$
	Field desc$
End Type

Global ITEM_ID=1

Function NEW_ITEM(b_type,x,y,ID=0)
	I.ITEM=New ITEM
	I\x=x
	I\y=y
	I\b_type=b_type
	Select I\b_type
		Case ITEM_JUMPSUIT
			I\I_TYPE=ITEM_CLOTHING
			I\name$="green jumpsuit"
			I\desc$="A standard uniform worn by all crewmembers of Space Station 14."
		Case ITEM_JUMPSUIT_1
			I\I_TYPE=ITEM_CLOTHING
			I\name$="pink jumpsuit"
			I\desc$="Intended for use by security personnel, it makes you stand out in a crowd."
		Case ITEM_CLOWN_MASK
			I\I_TYPE=ITEM_MASK
			I\name$="clown mask"
			I\desc$="A mask for clowns."
		Case ITEM_BIKE_HORN
			I\I_TYPE=ITEM_ACTIVABLE
			I\name$="bike horn"
			I\desc$="The most essential tool of a clown, making any situation at least ten times funnier."
		Case ITEM_CLOWN_SHOE
			I\I_TYPE=ITEM_SHOES
			I\name$="clown shoes"
			I\desc$="They are annoying."
		Case ITEM_SEC_BATON
			I\I_TYPE=0
			I\name$="security baton"
			I\desc$="Long, thick, black, and ready to penetrate some criminals."
	End Select
	If ID=0
		I\ID=ITEM_ID
		ITEM_ID=ITEM_ID+1
	Else
		I\ID=ID
	EndIf
End Function

Function GET_ITEM(ID)
	For I.item=Each item
		If I<>Null And I\ID=ID Then Return Handle(I)
	Next
	Return 0
End Function

;big huge item list
Const ITEM_JUMPSUIT=1
Const ITEM_JUMPSUIT_1=2
Const ITEM_CLOWN_MASK=3
Const ITEM_BIKE_HORN=4
Const ITEM_CLOWN_SHOE=5
Const ITEM_SEC_BATON=6