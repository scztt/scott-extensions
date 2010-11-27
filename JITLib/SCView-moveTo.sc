+ SCView {
	moveBy {
		| x, y |
		this.bounds_( this.bounds.moveBy( x,y ) )
	}
	
	moveTo {
		| x, y |
		this.bounds_( this.bounds.moveTo( x,y ) )
	}
}

+ SCContainerView {
	moveBy {
		| x, y |
		super.moveBy( x, y );
	}	
	
	moveTo {
		| x, y |
		var bounds = this.bounds;
		this.moveBy( x-bounds.left, y-bounds.top );
	}
	
	resizeToChildren {
		var bounds = Rect(0,0,0,0);
		children.do({
			| c |
			bounds = bounds.union( c.bounds );
		});
		this.bound_( bounds );
	}
}