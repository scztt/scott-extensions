+ ArrayedCollection {
		
	plot2D { arg name="plot", bounds;
		var window = SCWindow(name, bounds ?? { Rect(128, 64, 360, 360) });
		window.drawHook = { 
			Pen.addField(this, window.bounds) 
		};
		^window.front;
	}

}

+ Pen {
	
	*addField { arg array, bounds, selector=\fillRect, colorFunc=Color.grey(_), legato=1.0;
		var rows, cols, width, height, y, l;
		if(array.rank != 2) {ÊError("array not a 2D matrix").throw };
		#rows, cols = array.shape;
		height = bounds.height;
		width = bounds.width;
		this.use {
				rows.do { |i|
					cols.do { |j|
						var y = array[i][j];
						colorFunc.(y).set;
						l = legato.(y);
						this.perform(selector,
							Rect(
								width / cols * j, 
								height / rows * i, 
								width / cols * l + 1, // "trapping"
								height / rows * l + 1
							)
						);
					}
				}
			};

		}

}

