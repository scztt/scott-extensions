
TraceWindow {
	classvar <window, list, <items, <itemModifications;
	classvar testVal1;
		
	*guiTrace {
		| name, value |
		var return;
		items.isNil.if({
			items = Dictionary.new;
		});
		itemModifications.isNil.if({
			itemModifications = Dictionary.new;
		});
		window.isNil.if({
			this.makeWindow;
		});
		
		if( itemModifications.includesKey( name ), {
			items[ name ] = return = itemModifications[name].value( value )
		},{
			items[ name ] = return = value

		});
		this.refreshView;
		^return
	}
	
	*makeWindow {
		window = SCWindow( "Trace", Rect( 100,100, 600, 400 ) )
			.onClose_({ window = nil; list = nil });
		SCButton( window, Rect( 10, 5, 60, 18 ) )
			.states_([[ "clear", Color.black, Color.grey.alpha_(0.5) ]])
			.action_({
				items = Dictionary.new;
				this.refreshView;
			});
		SCButton( window, Rect( 80, 5, 60, 18 ) )
			.states_([[ "inspect", Color.black, Color.grey.alpha_(0.5) ]])
			.action_({
				this.items[ items.keys(List).at( list.value ) ].inspect; 
				this.dialogFor( items.associationAt( items.keys(List).at( list.value ) ) );
			});
		list = SCListView( window, Rect(0,20,580,400).insetBy(10,10) )
			.font_( Font( "Helvetica", 9 ) );
		window.front;
	}
	
	*dialogFor {
		| assoc |
		var w, name, valueView, funcView, function;
		assoc;
		w = SCWindow( assoc.key.asString, Rect( 100,100, 400,200 ) );
		valueView = SCTextView( w, Rect(5,5, 400-10, 80 ) )
			.string_( assoc.value.asString );
		SCStaticText( w, Rect( 30, 82, 150, 20))
			.string_("Change value to:");
		if( itemModifications.includesKey( assoc.key ), {
			function = itemModifications[ assoc.key ];
		});
		funcView = SCTextView( w, Rect( 5,100, 400-10, 45) )
			.string_( function.isNil.if({ assoc.value.asString },{function.asCompileString}) );
		SCButton( w, Rect( 30, 150, 25, 18 ) )
			.states_([[ "->", Color.black, Color.grey.alpha_(0.5) ]])
			.action_({ 
				{ function = funcView.string.compile.value }.try;
				function.isNil.not.if({
					itemModifications[ assoc.key ] = function;
					
				})
			});
		w.front;
	}
	
	*refreshView {
		var itemsAsString;
		itemsAsString = Array.new(items.size);
		items.keysValuesDo({
			|name, value|
			itemsAsString.add( name.asString ++ ": " ++ value.asString );
		});
		list.items_( itemsAsString );
	}
	
	*test {
		| v |
		testVal1 = v;
		testVal1.gtrace( "test" );
	}
}

