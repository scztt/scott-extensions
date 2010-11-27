ProxyGui {
	var node, nodename, parent, controller, origin;
	var background, title, power;
	var xPos=0, space=3, pBounds;
	var refresh, refreshing = false;
	
	var lightGrey, darkGrey, lightBorder, transGrey, powerOn, powerOff;
	var defaultFont, boldFont;


	*new {
		| node, nodename, parent, controller |
		^super.newCopyArgs( node, nodename, parent, controller )
	}
	
	makeTitle {
		title = 
			SCDragSource( background, Rect( 0, 1, 90, pBounds.height-2 ))
				.string_( nodename )			// @todo
				.stringColor_( lightGrey )
				.font_( boldFont )
				.align_(\center)
				.beginDragAction_({
					node
				});
		xPos = xPos + title.bounds.width + space; 
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
//				.relativeOrigin_(true)
	}
	
	makeEditButton {
		SCUserView( background, Rect( xPos, 0, 8, pBounds.height ) )
//			.relativeOrigin_(true)
			.focusColor_( transGrey  )
			.drawFunc_({
				Pen.fillColor_( transGrey );
				Pen.fillRect( Rect( 0, 0, 8, pBounds.height) );
				Pen.fillColor_( lightGrey );
				Pen.push;
					Pen.translate( 7, (pBounds.height/2) );
					Pen.moveTo( 0@0 );
					Pen.lineTo( 5.neg@3.neg );
					Pen.lineTo( 5.neg@3 );
					Pen.lineTo( 0@0 );
					Pen.fill;
				Pen.pop;
			})
			.mouseDownAction_({ |v| v.focus(true) })
			.mouseUpAction_({ |v| controller.createControlViewFor(node, nodename); v.focus(false); });
			xPos = xPos + 8; 
	}
	
	doRefreshLoop {
		| delta = 2 |
		background.onClose_({ refresh.stop });
		refreshing = true;
		refresh = SkipJack({this.refresh}, delta, {parent.isClosed} );
	}

	remove {
	}
}