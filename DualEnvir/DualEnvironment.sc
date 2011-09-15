DualEnvir : ProxySpace {
	var <nonNodes;
	var <>putAction;
	var window, <>linkedDoc, <>bufferDict;
	var nodeList;

	init { arg argServer, argName, argClock;
		nonNodes = Dictionary.new;
		argName = argName ? ("s" ++ (all.size + 1));
		//all.put( argName.asSymbol, this );
		super.init( argServer, argName, argClock );
	}
	
	at { arg key;
		(key.asString[1] == "_"[0]).if({
			^nonNodes.at( key );
		},{
			^super.at( key );
		})
	}
		
	put { arg key, obj;
		var ch;
		(key.asString[1] == "_"[0]).if({
			ch = key.asString[0];
			obj = case
				{ ch == $c } { case
					{ obj.class == ControlSpec } { ControlNode( server, key.asString, obj.asSpec )}
					{ true } { obj };
				}
				{ ch == $b } { case
					{ obj.class == Buffer } { obj }
					{ obj.class == String } { this.bufferFromPath(obj) }
					{ obj.isNumber } { Buffer.alloc( server, obj*44100 ) }
					{ obj.isArray } { Buffer.alloc( server, obj[0]*44100, obj[1], bufnum:obj[2] ) };
				}
				{ ch == ch } { obj };
			nonNodes.put( key, obj );
		},{
			super.put( key, obj );
		});
		this.putAction.value( key, obj);
		dispatch.value(key, obj); // forward to dispatch for networking
	}	
	
	bufferFromPath {
		| path |
		var buffer;
		path = path.standardizePath.toLower;
		bufferDict = bufferDict ? Dictionary.new;
		buffer = Buffer.read( server, path, bufnum: bufferDict[path] );
		bufferDict[path] = buffer.bufnum;
		^buffer
	}

	localPut { arg key, obj;
		(key.asString[1] == "_"[0]).if({
			nonNodes.put( key, obj );
		},{
			super.put( key, obj );
		})
	}	
	
	printOn { arg stream;
		stream << this.class.name;
		if(envir.isEmpty && nonNodes.isEmpty) { stream << " ()\n"; ^this };
		stream << " ( " << (name ? "") << Char.nl;
		this.keysValuesDo { arg key, item, i;
			stream << "~" << key << " - "; 
			stream << if(item.rate === 'audio') { "ar" } { 
					if(item.rate === 'control', { "kr" }, { "ir" })
					}
			<< "(" << item.numChannels << ")   " << if(i.even) { "\t\t" } { "\n" };
		};
		nonNodes.keysValuesDo { arg key, item, i;
			stream << "~" << key << " - "; 
			stream << item.class.asString << "\n" ;
		};
		stream << "\n)\n";
		
	}
	
	playButton {
		| w, rect | 
		var playbut;
		playbut = SCButton( w, rect );
		playbut.font = Font.new("LucidaSans", 10);
		playbut.states = [
			["playing", Color.black, Color.green.blend( Color.grey, 0.9)],
			["stopped", Color.black, Color.red.blend( Color.grey, 0.9)] ];
		playbut.action = {
			| b |
			b.postln;
			(b.value==0).if({
				"playing holder".postln;
				this.play;
			},{
				"stopping holder".postln;
				this.stop;
			});
		};

	}
	
	gui {
		| w |
		var curdoc, f, layout, newButton, editButton, playButton, n=0;
		curdoc = Document.current;
		window = w ?? MultiPageLayout( "proxyspace display", minWidth: 400 );
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
		window.startRow;

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
				
		// Populate nodeList
		window.startRow;

		// Audio first
		this.envir.keysValuesDo({
			|key, node|
			if( (node.bus.rate == \audio), { 
				node.makeNodeDisplay( window.view, key, n.even.if({ Color.new( 0.5,0.8,0.7,0.2 ) }, { Color.new( 0.95,1,0.95,0.2)} )); 
				n = n+1;
			})
		});
		n=0; window.startRow;
		// Audio first
		this.envir.keysValuesDo({
			|key, node|
			if( (node.bus.rate == \control),{
				node.makeNodeDisplay( window.view, key, n.even.if({ Color.new( 0.5,0.77,0.65,0.2 ) }, { Color.new( 0.95,1,0.95,0.2)} ));
				n = n+1;
			 })
		});
				
		// Make sure list stays updated
		this.putAction = { this.updateNodeList };
		
		window.resizeToFit.front;
	}
	
	updateNodeList {
		window.close; this.gui;
	}
		
	makeNodeProxyDocument {
		| name, contents |
		^"~" ++ name ++ " = " ++ contents;
	}
	
	editNodeProxy {
		| name, contents, origDocument=nil |
		var doc;
		var start, end, out;
		doc = Document.new(name, this.makeNodeProxyDocument(name, contents) );
		doc.alwaysOnTop = true;
		doc.promptToSave = false;
		doc.onClose = {
			thisProcess.interpreter.interpret(doc.string);
			(origDocument!=nil).if({
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
		| page, symbol, node |
		var playButton;
		// name
		ActionButton.new( page, "~" + symbol.asString + "." + (node.bus.rate==\control).if({"kr"},{"ar"}) + 
			"(" + node.bus.numChannels + ")", 
			{ this.findNodeDef( node ) }, 50);
		playButton = SCButton( page, page.layRight( 20,17 ) )
			.states_([[">", Color.green, Color.grey],["||",Color.blue, Color.grey]])
			.action_({ | button |
				( button.value==0 ).if({
					node.stop;
				},{
					node.play;
				})
			});
		node.monitor.isNil.not.if({ 
			(node.monitor.synthIDs.size>0).if({
				playButton.value = 1
			})
		})
	}
	
	makeNonNodeDisplay {
	}
	
	busEnvControl {
		| name, initial=0.0, min=0.0, max=1.0 ... mapTo |
		var node;
		name = name ? ("cont" ++ 100.rand);
		node = NodeProxy.busEnvelope( server, initial, min, max, name);
		envir.put( name.asSymbol, node );
		this.putAction.value( name.asSymbol, node);
		(name ++ " mapped to:").post;
		mapTo.do({
			| mapee |
			mapee.map( name.asSymbol, node );
			(envir.findKeyForValue( mapee ).asString ++ ",").post ;
		});
	}

	findNodeDef {
		| nodeName |
		var source;
		
		linkedDoc.string.findAll( this.findKeyForValue(nodeName).asString , true);
	}
	
	getAllControls {
		| ignoreControls, ignoreNodes |
		var allControls, someControls;
		
		ignoreControls = ignoreControls ? [ \out, \gate, \fadeTime ];
		ignoreNodes = ignoreNodes ? [];
		
		allControls = List(0);
		this.keysValuesDo({
			| key, node |
			ignoreNodes.includes( key ).not.if({
				allControls = allControls ++ node.getAllControls(ignoreControls);
			})
		})
		^allControls
	}

     linkDoc { arg doc, pushNow=true;
     	doc = doc ? Document.current;
     	linkedDoc = doc;
	if(pushNow) { this.push };
	"added actions to current doc".inform;
	doc.toFrontAction_({ this.push })
		.endFrontAction_({ this.pop });
     }
}

+Buffer {
	ar {
		arg rate=1.0, trigger=1.0, startPos=0.0, loop = 0.0, doneAction=0;
		^PlayBuf.ar( numChannels, bufnum, BufRateScale.kr(bufnum), trigger, startPos, loop, doneAction )
	}
}