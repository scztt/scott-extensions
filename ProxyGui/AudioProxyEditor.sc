AudioProxyEditor {
	var <proxy, proxyname, parent, controller;
	var <view, titleView, controls;
	var xPos = 0, xPos = 0;
	var lightGrey, darkGrey, lightBorder, transGrey, powerOn, powerOff;
	var defaultFont, boldFont;
	var proxyHash=0;

	*new {
		| proxy, name, view, controller |
		^super.newCopyArgs(proxy, name, view, controller).init
	}
	
	init {
		var b, p1;
		defaultFont = Font("LucidaSans", 9);
		boldFont = Font("LucidaSans-Demi", 9);

		controls = IdentityDictionary.new;

		this.initColors;
		view = VerticalStackedView( parent, 0@0, proxyname );
		this.makeTitle;
		this.addCloseButton;
		this.addUndockButton;
		
		this.refresh;
		view.refresh; view.refreshPositions;
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

	makeTitle {
		titleView = 
			SCDragSource( view, Rect( 0, 1, 190, 20 ))
				.string_( proxyname )			// @todo
				.stringColor_( lightGrey )
				.font_( boldFont )
				.align_(\center);
				
		xPos = xPos + titleView.bounds.width ; 
		^titleView
	}
	
	addCloseButton {
		var b, p1;
		view.addButton(
		{ |view|
			b = view.bounds.moveTo(0,0);
			p1 = b.width-4.5;
			
			darkGrey.set;
			Pen.fillOval( b );
			
			lightGrey.set;
			Pen.line( 4.5@4.5, p1@p1 ); Pen.perform([\stroke]);
			Pen.line( 4.5@p1, p1@4.5 ); Pen.perform([\stroke]);
		}, { "close".postln });
	}
	
	addUndockButton {
		var b, p1;
		view.addButton(
		{ |view|
			b = view.bounds.moveTo(0,0);
			
			darkGrey.set;
			Pen.fillOval( b );
			
			lightGrey.set;
			Pen.strokeRect( b.insetAll( 3.5, 4.5, 3.5, 4.5 ) ); 
		}, { "undock".postln });
	}	
	
	removeSelf {
		controller.removeControlView( this );
		//parent.refresh;
	}
	
	refresh {
		var newHash, newCont, controlSymbol, c, oldSize;
		newHash = proxy.source.hash;
		// proxy source has been changed - meaning possibly controls are diff
		if( (newHash==proxyHash).not, {  			
			proxyHash = newHash;
			oldSize = controls.size;
			
			newCont = IdentityDictionary.new;
			proxy.controlNames.do({
				| cn |
				controlSymbol = cn.name;
				c = controls[ controlSymbol ];
				if( c.isNil, {
					// Add a new control;
					newCont[controlSymbol] = ControlView( controlSymbol, this, controller );
				},{
					// Already exists; move over from old controls list
					newCont[controlSymbol] = c; controls[controlSymbol] = nil;
				})
			});
			
			// Do remove stuff on remaining controls (i.e. deleted ones)
			controls.keysValuesDo({
				| key, val |
				// @todo: remove stuff
			});

			view.prRefreshPositions;
			( oldSize == controls.size).not.if({ parent.view.refresh});
			controls = newCont;
			//this.refreshChildren;
		});
	}
	
	refreshChildren {
		controls.keysValuesDo({ |k, v| v.refresh });
	}
}

