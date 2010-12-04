+ Document {

	*getCommandBox {
		var d;
		d = this.new("enter command to interpret"," \n");
		d.stringColor_(Color.gray(0.0,0.0), 0, 5);
		d.stringColor_(Color.black, 5, 2);
		d.background = Color.gray(0.8, 0.95);
		d.bounds = Rect(310, 800, 600, 55);
		d.front;
		d.promptToSave = false;
		
		d.keyDownAction = {
			 | a, b, c|
			 if(b.ascii==3 && c==256, {
				a.string = " \" \"; ";
				a.stringColor_(Color.gray(0.0,0.0), 0, 5);
				a.stringColor_(Color.black, 5, 2);
				a.selectRange(5, 0);
				a.bounds = Rect(310, 800, 600, 55);
				a.string.interpret.postln;
			 });
			 if(b.ascii==13, {
				a.bounds = a.bounds.resizeBy(0,15);
			 });
		};
	}

	*maximizeAll {
		var done, screen, count, rects;
		done = false;
		screen = Rect(0,0,1200,854);
		count = 0;
		
		rects = Array.fill(allDocuments.size, {
			|n|
			if( allDocuments[n].isListener, {
				allDocuments[n].bounds.set(5,10, 300, 850);			},{
				allDocuments[n].bounds.resizeTo(25, 25);
			});
			
		}) ;
		
		while( { done.not; }, {
			done = true;

			rects.size.do({
				| n |
				var okay, rect, newr;
	
				//left
				newr = rects[n];
				newr = newr.moveBy(-1,0);
				okay = true;
				rects.do({ |crect|
					(crect != rects[n]).if({
						(crect.bounds.intersects(newr)).if({
							okay = false;
						});
					});
				});
				(okay && screen.containsRect(newr) ).if({
					rects[n] = newr;
					done = false;
				});
				//-----

				//right
				newr = rects[n];
				newr = newr.resizeBy(2,0);
				okay = true;
				rects.do({ |crect|
					(crect != rects[n]).if({
						(crect.bounds.intersects(newr)).if({
							okay = false;
						});
					});
				});
				(okay && screen.containsRect(newr) ).if({
					rects[n] = newr;
					done = false;
				});
				//-----

				//top				
				newr = rects[n];
				newr = newr.moveBy(0,-1);
				okay = true;
				rects.do({ |crect|
					(crect != rects[n]).if({
						(crect.bounds.intersects(newr)).if({
							okay = false;
						});
					});
				});
				(okay && screen.containsRect(newr) ).if({
					rects[n] = newr;
					done = false;
				});
				//-----
				
				//top
				newr = rects[n];
				newr = newr.resizeBy(0,2);
				okay = true;
				rects.do({ |crect|
					(crect != rects[n]).if({
						(crect.bounds.intersects(newr)).if({
							okay = false;
						});
					});
				});
				(okay && screen.containsRect(newr) ).if({
					rects[n] = newr;
					done = false;
				});
				//-----
				
				
			});
			count = count + 1;
		});
		allDocuments.size.do({
			|n|
			allDocuments[n].bounds = rects[n].insetBy(3,10);
		});
	}
	
	selectBlock {
		| a=$(, b=$) |
		var i, string, inParens=0, outParens=0, c, selectionStart, selectionEnd;
		string = a.asString + this.string + b.asString;
		
		selectionStart = this.selectionStart+2;
		selectionEnd = this.selectionStart+1;
		
		while( { ((inParens-outParens) < 1)  }, {
			selectionStart = selectionStart-1;
			c = string[selectionStart];
			if( c == a , { inParens = inParens+1 } );
			if( c == b , { outParens = outParens+1 } );
		});

		while( { ((outParens-inParens) != 0)  }, {
			selectionEnd = selectionEnd+1;
			c = string[selectionEnd];
			if( c == a , { inParens = inParens+1 } );
			if( c == b , { outParens = outParens+1 } );
		});
			
		this.selectRange( selectionStart-2, selectionEnd-selectionStart+1 );
	}
	
	proxyBold {
		var string, tildes;
		this.font_( Font.new("LucidaSans", 11), -1, 1 );
		string = this.string;
		tildes = string.findAll( "~" );
		tildes.do({
			| i |
			var d=1;
			while( { string[i+d] == "_"[0] || string[i+d].isAlphaNum }, {
				d=d+1;
			});
			this.font_( Font.new("LucidaSans-Demi", 11), i, d );
		});
		this.syntaxColorize
	}
	
	syntaxColorize {
		_TextWindow_SyntaxColorize;
	}
	
	zoom {
	}
}	

