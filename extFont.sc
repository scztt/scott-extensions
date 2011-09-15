+ Font {
	*guiAvailableFonts {
		SCListView.new( SCWindow("", Rect(0,0,200,500)).front, Rect(0,0,200,500))
			.items_( Font.availableFonts.collect( _.asString).sort({ |a,b| a<b }) );
	}
}
