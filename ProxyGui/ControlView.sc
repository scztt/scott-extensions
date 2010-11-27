ControlView {
	var name, parent, controller;
	var background, title, source, sourceMenu, value, pBounds; 
	var proxy, type;
	var xPos=0, yPos=0, space=2;
	var lightGrey, darkGrey, lightBorder, transGrey, powerOn, powerOff;
	var defaultFont, boldFont;
	var numericValue = false, oldHash=0;
	
	*new {
		| paramName, parent, controller |
		^super.newCopyArgs( paramName, parent, controller ).init
	}
	
	init {
		defaultFont = Font("LucidaSans", 9);
		boldFont = Font("LucidaSans-Demi", 9);

		pBounds = Rect(0,0, 400, 22);

		this.initColors;
		this.makeBackground;
		this.makeTitle;
		this.makeSource;
		this.makeValue;
		background.bounds_( background.bounds.width_( xPos ) );
		this.refresh;
	}
	
		// Some colors:
	initColors {
		lightGrey = Color.grey( 0.95 );
		lightBorder = Color.grey(0.8, 1);
		darkGrey = Color.grey( 0.3, 1 );
		transGrey = Color.grey( 1, 0.2 );
		powerOn = Color.green;
		powerOff = Color.green(0.5, 1);
	}
	
	makeBackground {
		background = 
			SCCompositeView( parent.view, pBounds )
				.background_( Gradient( darkGrey, Color.grey(0.4, 1), 0, 10 ) )
				.relativeOrigin_(true)
	}
	
	makeTitle {
		title = 
			SCDragSource( background, Rect( 0, 1, 90, pBounds.height-2 ))
				.string_( name )			// @todo
				.stringColor_( lightGrey )
				.font_( boldFont )
				.align_(\center);
				
		xPos = xPos + title.bounds.width + space; 
	}
	
	makeSource {
		source = 
			SCDragSink( background, Rect( xPos, 1, 120, pBounds.height-2) )
				.string_( "(def)" )
				.stringColor_( lightGrey )
				.font_( boldFont )
				.align_(\center)
				.canReceiveDragHandler_({
					| view |
					( view.class.currentDrag.isKindOf( NodeProxy ) ||
						view.class.currentDrag.isKindOf( Bus ) ||
						view.class.currentDrag.isKindOf( Number ) )
				})
				.receiveDragHandler_({
					| view |
					var drag = view.class.currentDrag;
					case
						{ drag.isKindOf( NodeProxy ) } 
							{ parent.proxy.map(name.asSymbol, drag) }
						{ drag.isKindOf( Bus ) } 
							{ parent.proxy.map(name.asSymbol, drag) }
						{ drag.isKindOf( Number ) } 
							{ parent.proxy.set( name.asSymbol, drag ) };
					this.refresh;
				});
		xPos = xPos + source.bounds.width + space; 
		sourceMenu = 
			SCButton( background, Rect( xPos-11-space, 2, 10, 8 ) )
				.states_([[ "x", Color.black, Color.clear ]])
				.font_(Font("Helvetica", 7))
				.canFocus_(false)
				.action_({
					parent.proxy.unmap(name.asSymbol).unset(name.asSymbol) ;
					this.refresh;
				});
	}
	
	makeValue {}
	
	refresh {
		var nodeMap, hash;
		nodeMap = parent.proxy.nodeMap[name];
		if( (oldHash == (oldHash = nodeMap.hash)).not, { 
			source.string = 
				nodeMap.notNil.if({
					nodeMap.value.isKindOf( Number ).if({
						// is a number
						numericValue = true;
						nodeMap.value
					},{
						// is mapped
						DualEnvir.findPathToValue( nodeMap.value ).asString.reverse[0..22].reverse
					})
				},{ "(def)" });
		});
	}
}