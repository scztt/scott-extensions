//e = [0,0,0,0];
//a = (-45 + ( [0,1,2,3]*(360/4) )) % 360;
//Kemar.path = "KemarHRTF/"
//Kemar.initBuffers(a, e);

HeadphoneAmbi {
	*ar {
		| in, az |
		LPF.ar( PanB.ar( in, az, 0), 22000-((az.abs)*17000), (1 - (az.abs*0.25)) )
	}
}

HeadphoneAmbiOut {
	*ar {
		| w, x, y, mul=1.0 |
		Kemar.ar(	
			DecodeB2.ar(4, w, x, y, 0.5) );
	}
}