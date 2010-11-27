/*
+ DualEnvir {
	
	gui {
		var curdoc, f, window, layout, newButton, editButton, playButton, nodeList;
		curdoc = Document.current;
		window = SCWindow.new( "ProxySpace gui" );
		window.view.decorator = FlowLayout(window.view.bounds);
		window.alwaysOnTop = true;
		window.onClose = {
			this.putAction = nil;
		};
		
		// --------- NEW ---------
		newButton = SCButton.new( window, Rect(0, 0, 40, 20) )
			.states = [["new", Color.black, Color.gray ]];
		newButton.action = {
			this.editNodeProxy("new","{\n\t\n}");
		};
		
		// --------- EDIT ---------
		editButton = SCButton.new( window, Rect( 0, 0, 40, 20 ) )
			.states = [["edit", Color.black, Color.gray ]];
		editButton.action = {
			| list |
			var node;
			node = this.envir.at( nodeList.item.asSymbol );
			this.editNodeProxy( nodeList.item.asString, node.source.def.sourceCode );
		};
		window.view.decorator.nextLine;

		// --------- PLAY ---------
		playButton = SCButton.new( window, Rect(0,0, 20, 20) )			.states = [[ ">", Color.green, Color.grey ],
					  [ "||", Color.black, Color.grey ]];
		playButton.action = {
			var node;
			node = this.envir.at( nodeList.item.asSymbol );
			(node.monitor == nil).if( {
				node.play;
			},{
				node.end;
			});
		};
		
		// --------- LIST ---------
		nodeList = SCListView.new( window, Rect(0,0,300,500) );
		nodeList.action = {
			var node;
			node = this.envir.at( nodeList.item.asSymbol );
			(node.monitor == nil).if( {
				playButton.value = 0;
			},{
				playButton.value = 1;
			});
		};

		// Populate nodeList
		this.envir.keys.do({
			|item|
			nodeList.add(item);
		});
				
		// Make sure list stays updated
		f = { 
			"tesT".postln;
			nodeList.items = [];
			this.envir.keys.do({
				|item|
				nodeList.add(item);
			});
		};
		this.putAction = f;
		
		window.bounds = window.view.decorator.currentBounds.resizeBy( 5, 5 );
		window.front;
	}
		
	makeNodeProxyDocument {
		| name, contents |
		^"~" ++ name ++ " = " ++ contents;
	}
	
	editNodeProxy {
		| name, contents, origDocument=nil |
		var doc;
		doc = Document.new(name, this.makeNodeProxyDocument(name, contents) );
		doc.alwaysOnTop = true;
		doc.promptToSave = false;
		doc.onClose = {
			thisProcess.interpreter.interpret(doc.string);
			(origDocument!=nil).if({
				var start, end, out;
				start = origDocument.string.find( contents );
				end = start + contents.size;
				out = origDocument.string.copyRange(0, start);
				out = out ++ doc.string;
				out = out ++ origDocument.string.copyRange( end, origDocument.string.size);
				origDocument.string = out;
			});
		};
	}
	
	makeNodeDisplay {
		Page
	}
	
	makeNonNodeDisplay {
	}
}
*/