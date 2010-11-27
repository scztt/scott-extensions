CollapsableView : StretchView {
	var contains;
	var mainView, <itemsView, marginView;
	var origin, margin=3, leftMargin=12, oldBounds; 
	var cBackground, cLightBackground, cLines;
	var <>arrowDown=10;
		
	initColors {
		cLightBackground = Color.grey(0.4);
		cBackground = Color.grey( 0.15 );
		cLines = Color.grey( 0.9 );
	}

	*new {
		| parent, origin, name="" |
		^super.new.init( parent, origin, name );
	}
	
	init {
		| pr, or, nm |
		parent = pr;
		origin = or ? (0@0);
						
		this.initColors;
		this.initView;
		if( parent.isKindOf(StretchView), { parent.add( this ) });
		parent.refresh; this.prRefreshPositions;
	}
	
	asView { ^itemsView }
	
	add {
		|view|
		
	}
	
	moveTo {
		| x, y |
		origin = x@y;
		mainView.moveTo( x, y );	
	}
	
	initView {		
		mainView = SCCompositeView( parent.view, Rect( origin.x, origin.y, 10, 10) )
//			.relativeOrigin_(true)
			.background_( cBackground );			
				
		itemsView = SCCompositeView( mainView, Rect( leftMargin, 0, 10, 10 ) )
//			.relativeOrigin_( true )
			.background_( cLightBackground );

		marginView = SCUserView( mainView, Rect( 0, mainView.bounds.height/2 - 16, leftMargin, 32 ) )
			.canFocus_( false )
//			.relativeOrigin_(true)
			.drawFunc_({
				|view|
				cLines.set;
				Pen.push;
					Pen.translate( (leftMargin/2).round(1), 16 );
					
					if( minimized.not, {
						Pen.rotate( pi, 0, 0 );
					});
					Pen.moveTo( 2@0 );
					Pen.lineTo( 2.neg@4.neg );
					Pen.lineTo( 2.neg@4 );
					Pen.lineTo( 2@0 );			
					Pen.fill;
				Pen.pop;
			})
			.mouseUpAction_({
				minimized = minimized.not;
				if( minimized, {
					oldBounds = mainView.bounds;
					mainView.bounds_( mainView.bounds.width_(leftMargin+1) );
					itemsView.visible_( false );
					parent.refresh; parent.refreshPositions;
				},{
					mainView.bounds_( oldBounds );
					itemsView.visible_( true );
					parent.refresh; parent.refreshPositions;
				});
			});

		this.refresh;
	}
	
	bounds {
		^mainView.bounds;
	}
	
	bounds_{
		| rect |
		mainView.bounds( rect );
	}
	
	view {
		^itemsView;
	}
	
	setInnerExtent{
		| w, h |
		mainView.setInnerExtent( w, h );
		marginView.bounds = marginView.bounds.moveTo( 0,mainView.bounds.height/2 - 16 );
		itemsView.setInnerExtent( w-leftMargin, h );
		parent.refresh;
	}

	prRefreshPositions {
		var w=10, h=10, b;
		format( "% (%): refreshed", this.class,this.bounds).postln;
		itemsView.children.do({
			|view|
			b = view.bounds;
			w = w.max(b.right);
			h = h.max(b.bottom);
		});
		this.setInnerExtent( w+leftMargin, h );
	}	
}

