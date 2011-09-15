Glossary {
	var <>dict, w;
	var dir = "/Users/fsc/Documents/Music/scwork/glossary/";
	
	*new {
		^super.new.init;
	}
	
	init {
		dict = Dictionary.new;
	}
	
	loadFromDir {
		| path, key, value |
		var files;
		files = path.pathMatch;
		
		files.do({
			| filename |
			key = filename.split($/).reverse[0].split($.)[0];
			value = File( filename, "r").readAllStringRTF;
			dict.put( key, value );
		})
	}
	
	gui {
		var nameBox, contentBox, height;
		height  = 10 + (dict.keys.size*(22+5) + 22 + 5);
		PageLayout.bgcolor = Color.new(0.91,0.91,0.82);
		w = PageLayout( "Glossary", 200, height, 
			Document.current.bounds.left, 
			Document.current.bounds.bottom - height - 45,
			5, 5 );
		dict.keys.do({
			|k|
			SCButton( w.window, w.layDown( 190, 22 ) )
				.states_([[ k.asString, Color.black, Color.grey( 0.5, 0.1 ) ]] )
				.action_({
					| view |
					this.insert( k )
				});
		});
		w.startRow.startRow;
		SCButton( w.window, w.layDown( 190, 22 ) )
			.states_([[ "(new from selection)", Color.black, Color.grey( 0.7, 0.3 ) ],
					[ "save", Color.black, Color.grey( 0.7, 0.3)] ])
			.font_( Font( "HelveticaNeue-Italic", 12 ) )
			.action_({
				| view |
				(view.value == 1).if({
					SCStaticText( w.window, w.layDown( 190, 22 ) )
						.string_("replace...");	
					w.indent(1);	
					nameBox = SCTextView( w.window, w.layDown( 160, 22 ) )
						.enterInterpretsSelection_(false);					w.indent(-1);
					SCStaticText( w.window, w.layDown( 190, 22 ) )
						.string_("with...");	
					contentBox = SCTextView( w.window, w.layDown( 190, 400 ) )
						.string_( Document.current.selectedString )
						.hasHorizontalScroller_( true )
						.enterInterpretsSelection_(false);
					w.resizeToFit;				
				},{
					this.saveReplacement( nameBox.string, contentBox.string );
					w.close;
				})
			});
		w. front;
	}
	
	insert {
		| key |
		Document.current.string_( dict[key], 
			Document.current.selectionStart, 
			Document.current.selectionSize );
		Document.current.syntaxColorize.proxyBold;
		w.close;
		w = nil;
	}
	
	saveReplacement {
		| key, value |
		File.new( dir ++ key.stripRTF ++ ".glossary", "w" ).putAll( value.stripRTF ).close;
		dict.put( key, value.stripRTF );
	}
}
