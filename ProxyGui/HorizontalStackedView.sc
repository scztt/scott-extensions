HorizontalStackedView : StackedView {
	var <>name, <contents;
	var mainView, itemsView, title, minMaxButton, border; 
	var origin, top=0, xPos, yPos, <>margin=3, space=6, oldBounds;
	var  header;
	
	*new {
		| parent, origin, name="", header=false |
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
		
		this.initColors;
		this.initView;
		if( parent.isKindOf(StackedView), { parent.add( this ) });
		parent.refresh; this.prRefreshPositions;
	}
	
	asView { ^itemsView }
	
	moveTo {
		| x, y |
		origin = x@y;
		mainView.moveTo( x, y );	
	}
	
	initView {		
		mainView = SCCompositeView( parent.view, Rect( origin.x, origin.y, 200, 300) )
//			.relativeOrigin_(true)
			.background_( cBackground );
				
		header.if({	
			title = SCStaticText( mainView, Rect( 0, 0, 180, 30 ) )
				.font_( Font("HelveticaNeue-Bold", 18) )
				.string_( "    " ++ name )
				.stringColor_( Color.grey( 0.9, 0.7 ) );
			SCUserView( mainView, title.string.bounds( title.font ).width_(120).moveToPoint( title.bounds.origin ) )
				.canFocus_( false )
				.drawFunc_({
					|view|
					cLines.set;
					Pen.push;
						Pen.translate( 8, (view.bounds.height/2).round + 5 );
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
		
		itemsView = SCCompositeView( mainView, Rect( 0, top, 200,300 ) )
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
		mainView.setInnerExtent( w, h );
		itemsView.setInnerExtent( w, h-top );
		mainView.bounds.height = itemsView.bounds.bottom;
		parent.refresh;
	}
	
	add {
		| view |
		contents.add( view );
		this.refresh;
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
		var tallest=50;
		format( "% (%): refreshed", this.class,this.bounds).postln;
		xPos = margin;
		contents.do({
			| val |
			val.moveTo( xPos, margin );
			xPos = xPos + val.bounds.width + space;
			tallest = val.bounds.bottom.max(tallest);
		});
		this.setInnerExtent( xPos - space + margin, tallest + top + margin  );
		parent.refresh;
	}
}

