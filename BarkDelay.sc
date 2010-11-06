BarkDelay : UGen {
	*ar { arg in, deltimes, feedback, amps, maxdelay=1.0, width=1.0, freqmul=1.0;
		var filterbank, delays,
		rq = [ 1.16, 0.38666666666667, 0.232, 0.16571428571429, 0.14177777777778,
			0.12210526315789, 0.116, 0.10357142857143, 0.0928, 0.094188034188033,
			0.088905109489053, 0.087, 0.087783783783783, 0.086325581395351, 0.08816,
			0.089999999999998, 0.093823529411763, 0.1015, 0.10875, 0.11,
			0.10771428571429, 0.12282352941176, 0.13809523809524, 0.15037037037037, 1.178125 ],
		cf = [ 50, 150, 250, 350, 450,
			570, 700, 840, 1000, 1170,
			1370, 1600, 1850, 2150, 2500,
			2900, 3400, 4000, 4800, 5800,
			7000, 8500, 10500, 13500, 16000 ] * freqmul;
		filterbank = BBandPass.ar( in + LocalIn.ar(1), cf, rq * width );
		delays = DelayN.ar( filterbank, maxdelay, deltimes - ControlRate.ir.reciprocal ) * amps;
		LocalOut.ar( (delays * feedback).sum );
		^delays
	}
}
