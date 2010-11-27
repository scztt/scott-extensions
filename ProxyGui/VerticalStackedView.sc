VerticalStackedView : StackedView {
	var <>name, <contents;
	var <mainView, itemsView, title, minMaxButton, headerButtons; 
	var origin, top=1, yPos, <>margin=3, space=3, oldBounds;
	var header;
	
	*new {
		| parent, origin, name="", header=true |
		^super.new.init( parent, origin, name, header );
	}
	
	init {
		| pr, or, nm, hd |
		parent = pr;
		origin = or ? (0@0);
		name = nm;
		header = hd;
		
		yPos = margin;
		
		contents = LinkedList.new;
		headerButtons = List.new;
		
		this.initColors;
		this.initView;
		if( parent.isKindOf(StretchView), { parent.add( this ) });
		parent.refresh; this.prRefreshPositions;
	}
	
	asView { ^itemsView }
	
	moveTo {
		| x, y |
		origin = x@y;
		mainView.moveTo( x, y );	
	}
	
	initView {		
		mainView = SCCompositeView( parent.asView, Rect( origin.x, origin.y, 200, 300) )
//			.relativeOrigin_(true)
			.background_( cBackground );

		header.if({
			title = SCStaticText( mainView, Rect( 1, 0, 180, 30 ) )
				.font_( Font("HelveticaNeue-Bold", 18) )
				.string_( "    " ++ name )
				.stringColor_( Color.grey( 0.9, 0.7 ) );
			SCUserView( mainView, title.string.bounds( title.font ).moveToPoint( title.bounds.origin ) )
				.canFocus_( false )
//				.relativeOrigin_(true)
				.drawFunc_({
					|view|
					cLines.set;
					Pen.push;
						Pen.translate( 8, (view.bounds.height/2).round + 4 );
						if( minimized, {
							Pen.moveTo( 2@0 );
							Pen.lineTo( 2.neg@4.neg );
							Pen.lineTo( 2.neg@4 );
							Pen.lineTo( 2@0 );				
						},{
							Pen.moveTo( 0@2 );
							Pen.lineTo( 4@2.neg );
							Pen.lineTo( 4.neg@2.neg );
							Pen.lineTo( 0@2 );
						});
						Pen.fill;
					Pen.pop;
				})
				.mouseUpAction_({
					minimized = minimized.not;
					if( minimized, {
						oldBounds = mainView.bounds;
						mainView.bounds_( mainView.bounds.height_(32) );
						itemsView.visible_( false );
						parent.refresh; parent.refreshPositions;
					},{
						mainView.bounds_( oldBounds );
						itemsView.visible_( true );
						parent.refresh; parent.refreshPositions;
					});
				});
			top = title.bounds.height;
		});
		
		itemsView = SCCompositeView( mainView, Rect( 1, top, 200,300 ) )
//			.relativeOrigin_( true )
			.background_( cLightBackground );

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
		[w,h].postln;
		mainView.setInnerExtent( w, h );
		itemsView.setInnerExtent( w-2, h-top-1 );
		mainView.bounds.height = itemsView.bounds.bottom;
		parent.refresh;
	}
	
	add {
		| view |
		contents.add( view );
		this.refresh;
	}
	
	addButton {
		| draw, action |
		var size = 14;
		headerButtons.add( 
			SCUserView( mainView, 
				Rect( mainView.bounds.width-((size+2)*(headerButtons.size+1)) - (title.bounds.height-size)/2, (title.bounds.height-size)/2, 
					size, size ))
			.relativeOrigin_(true)
			.mouseUpAction_( action )
			.drawFunc_( draw )
			.canFocus_( false ) 
		);
	}
	
	remove {
		| view |
		contents.remove( view.remove );
		this.refresh; parent.refresh;
		^view
	}
	
	prRemoveChild {
		| view |
		this.remove(view)
	}
	
	prRefreshPositions {
		var widest=50, oldH, oldW;
		format( "% (%): refreshed", this.class,this.bounds).postln;
		yPos = margin; oldW = this.bounds.width;
		contents.do({
			| val |
			val.moveTo( margin, yPos );
			yPos = yPos + val.bounds.height + space;
			widest = val.bounds.right.max(widest);
		});
		this.setInnerExtent( widest+(margin*2)-space+2, yPos + top + margin - 2 );
		parent.refresh; //mainView.refresh;
		if( (oldW==this.bounds.width).not, { this.headerButtonsRefresh });
	}
	
	headerButtonsRefresh {
		var size=14;
		headerButtons.do({
			| but, i |
			but.bounds_( Rect( mainView.bounds.width - ((size+2)*(i+1)) - 5, (title.bounds.height-size)/2, size, size ) );
		})
	}
}

StretchView {
	var refreshNeeded = false, minimized=false;
	var parent;
	
	refresh {
		refreshNeeded = true;
	}
	
	refreshPositions {
		if( minimized.not && refreshNeeded, {			
			this.prRefreshPositions;
			refreshNeeded = false;
			parent.refreshPositions;
		})
	}
}

StackedView : StretchView {
	var cBackground, cLightBackground, cLines;
	
	initColors {
		cLightBackground = Color.grey(0.4);
		cBackground = Color.grey( 0.15 );
		cLines = Color.grey( 0.9 );
	}
}


+SCWindow {
	prRefreshPositions {
		var w=10, h=10, b;
		format( "% (%): refreshed", this.class,this.bounds).postln;
		view.children.do({
			|view|
			b = view.bounds;
			w = w.max(b.right);
			h = h.max(b.bottom);
		});
		this.setInnerExtent( w, h );
	}
	refreshPositions { this.prRefreshPositions }
	refresh {  }
}

+SCView {
	refreshPositions {}
	prRefreshPositions{}
}