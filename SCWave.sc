SCWave {
	var window, views, viewStrings, viewOwners, textArea,
		addButton, delButtons,
		space=4;

	*new {
		^super.new.init;
	}
	
	init {
		views = List.new;
		viewStrings = List.new;
		viewOwners = List.new;
		delButtons = List.new;
	
		window = SCWindow( "SCWave", Rect( 100,100, 350, 100 ), resizable:true, scroll:true );
		window.view.hasHorizontalScroller = false;
		addButton = RoundButton( window, Rect( 350-35, 100-25, 25, 17 ))
			.resize_( 9 )
			.font_(Font("Helvetica",9))
			.states_([["+"]])
			.action_({ 
				| view |
				var newView, newButton, i;
				i = views.size;
				newView = SCTextView( textArea, Rect( 0, 0, 340-8, 18 ))
					.focusColor_( Color.yellow( 0.9, 0.3 ) )
					.action_({ "action".postln })
					.keyDownAction_({ "keydown".postln })
					.keyUpAction_({
						if( this.resizeIfNeeded( newView ), {
							this.reflowAll();
						});
						viewStrings[i] = newView.string;
					});
				views.add( newView );
				viewStrings.add( "" );
				viewOwners.add( \me );
				newView.focus(true);
				this.reflowAll;
			});
			
		textArea = SCCompositeView( window, Rect( 10,10,330,100-40 ) )
			.background_(Color.grey(0.3));
		window.front;
	}
	
	resizeIfNeeded {
		| view |
		view.string.bounds( view.font ).postln;
		view.bounds.height.postln;
		^if( ( view.bounds.height - view.string.bounds( view.font ).height ).abs < 4, {
			false
		}, {
			this.resizeView( view );
			true
		});
	}
	
	resizeView {
		| view |
		view.bounds = view.bounds.height_( view.string.bounds( view.font ).height );
	}
	
	resizeAllViews {
		views.do( resizeView(_) )
	}
	
	reflowAll {
		var x, y;
		x = 4; y = 4;
		views.do({
			| view |
			view.bounds = view.bounds.moveTo( x, y );
			y = y + view.bounds.height + space;
		});
		textArea.bounds = textArea.bounds.height_(y+1);
		window.bounds = window.bounds.height_( (y+65).min(450) );
		addButton.bounds = addButton.bounds.top_( textArea.bounds.bottom+5 );
	}
}