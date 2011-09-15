+ SCView {
	setInnerExtent {
		|w,h|
		this.bounds_( this.bounds.width_(w).height_(h) );
	}
	
	findWindow {
		if( this.parent.notNil, {
			^parent.findWindow
		},{
			SCWindow.allWindows.do { |win|
				if(win.view == this) {
					^win
				}
			}
		})
	}
}

+ QView {
	setInnerExtent {
		|w,h|
		this.bounds_( this.bounds.width_(w).height_(h) );
	}
	
	findWindow {
		if( this.parent.notNil, {
			^this.parent.findWindow
		},{
			QWindow.allWindows.do { |win|
				if(win.view == this) {
					^win
				}
			}
		})
	}
}