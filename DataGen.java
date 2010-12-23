
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;


public class DataGen {


    static final DecimalFormat DF_INT  = new DecimalFormat( "000"    );
    static final DecimalFormat DF_RET  = new DecimalFormat( "00.000" );
    static final DecimalFormat DF_PROB = new DecimalFormat( "0.000"  );

    static final int HI = 0;
    static final int MD = 1;
    static final int LO = 2;

    static final double MAX_RET = 40;

    static final Random RND = new Random();


    public static void main( String[] args ) throws IOException {
        if ( args.length != 2 ) {
            System.out.println(
                    "\tusage:  java DataGen <nGambles> <outFile>\n" );
            System.exit(1);
        }
        int ng = Integer.parseInt( args[0] );

	try{
	    BufferedWriter bw = new BufferedWriter(
						   new FileWriter( args[1] ) );
	    //  generate gamble returns & probs
	    bw.write( "#gamble(gambleid, high return, high prob, " +
		      "medium return, med prob, low return, low prob)" );
	    bw.newLine();
	    //  for each gamble...
	    for ( int i = 0; i < ng; i++ ) {
		//  keep probs b/w 10 & 90 %
		double[] probs = new double[3];
		probs[LO] = RND.nextDouble() * 0.8 + 0.1;
		probs[MD] = RND.nextDouble() * 0.8 + 0.1;
		probs[HI] = RND.nextDouble() * 0.8 + 0.1;
		//  normalize - probs will end up b/w 5 & 95 %
		double probsTotal = probs[HI] + probs[MD] + probs[LO];
		probs[LO] /= probsTotal;
		probs[MD] /= probsTotal;
		probs[HI] /= probsTotal;
		//  make avg return 2
		double[] rets = new double[3];
		//  init low return to b/w 0.05 & 1
		rets[LO] = RND.nextDouble() * 0.95 + 0.05;
		//  init med return to b/w 1 & 2.5
		rets[MD] = RND.nextDouble() * 1.5 + 1;
		//  init hi return so avg is 2
		rets[HI] = ( 2 - ( probs[MD]*rets[MD] +
				   probs[LO]*rets[LO] ) ) / probs[HI];
		//  depending on probabilities, rets[HI] calculated
		//  above could be really big, or below rets[MD]
		//  if either of these, redo
		if ( rets[HI] <= rets[MD] || rets[HI] >= MAX_RET || probs[MD] < 0.4) {
		    i--;
		    continue;
		}
		//  write to file
		bw.write( DF_INT.format(i) + ", " +
			  DF_RET .format( rets [HI] ) + ", " +
			  DF_PROB.format( probs[HI] ) + ", " +
			  DF_RET .format( rets [MD] ) + ", " +
			  DF_PROB.format( probs[MD] ) + ", " +
			  DF_RET .format( rets [LO] ) + ", " +
			  DF_PROB.format( probs[LO] ) );
		bw.newLine();
	    }
	    bw.newLine();
	    //  generate attributes
	    //  with 4 binary attrs, have 16 combos of these, so 16
	    //  distinct attr sets
	    //  want attr-sets to have a balanced distr
	    //  so each attr-set should occur ng/16 times
	    //  if ng not exactly divisible by 16, some should occur
	    //  floor(ng/16) times, and some should occur ceil(ng/16) times
	    //  create a list to hold ng attrs
	    ArrayList g2a = new ArrayList(ng);
	    //  add each attr-set floor(ng/16) times
	    int floor = ( int ) Math.floor( ng/16 );
	    for ( int i = 0; i < 16; i++ )
		for ( int j = 0; j < floor; j++ )
		    g2a.add( new Integer(i) );
	    //  fill any remaining slots in list with a permutation
	    //  of the attr-sets
	    int nRemaining = ng - 16*floor;
	    if ( nRemaining > 0 ) {
		ArrayList aperm = new ArrayList();
		for ( int i = 0; i < 16; i++ )
		    aperm.add( new Integer(i) );
		Collections.shuffle(aperm);
		for ( int i = 0; i < nRemaining; i++ )
		    g2a.add( aperm.get(i) );
	    }
	    //  permute the list so the attrs occur in a rnd order
	    Collections.shuffle(g2a);
	    //  write the attr-sets to file
	    bw.write( "#gambleatts(gambleid, gambleclass)" );
	    bw.newLine();
	    for ( int i = 0; i < ng; i++ ) {
		String s = null;
		switch ( ( ( Integer ) g2a.get(i) ).intValue() ) {
                case  0:  s = "0";  break;
                case  1:  s = "1";  break;
                case  2:  s = "2";  break;
                case  3:  s = "3";  break;
                case  4:  s = "4";  break;
                case  5:  s = "5";  break;
                case  6:  s = "6";  break;
                case  7:  s = "7";  break;
                case  8:  s = "8";  break;
                case  9:  s = "9";  break;
                case 10:  s = "10";  break;
                case 11:  s = "11";  break;
                case 12:  s = "12";  break;
                case 13:  s = "13";  break;
                case 14:  s = "14";  break;
                case 15:  s = "15";  break;
		}
		bw.write( DF_INT.format(i) + ", " + s );
		bw.newLine();
	    }
	    bw.newLine();
	    //  generate links
	    //  order gamble ids randomly
	    ArrayList gidperm = new ArrayList(ng);
	    for ( int i = 0; i < ng; i++ )
		gidperm.add( new Integer(i) );
	    Collections.shuffle(gidperm);
	    //  going to iterate thru this list, so need an iterator
	    Iterator it = gidperm.iterator();
	    //  start writing to file
	    bw.write( "#link(gambleid, gambleid)" );
	    bw.newLine();
	    //  link ~10% of gambles in 'quads'
	    //  how many quads?  4x = 10/100  =>  x = 1/40
	    int nq = ( int ) Math.round( ng/40.0 );
	    for ( int i = 0; i < nq; i++ ) {
		//  get ids for this quad
		String id1 = DF_INT.format( it.next() );
		String id2 = DF_INT.format( it.next() );
		String id3 = DF_INT.format( it.next() );
		String id4 = DF_INT.format( it.next() );
		//  create links b/w these ids
		bw.write( id1 + ", " + id2 );
		bw.newLine();
		bw.write( id1 + ", " + id3 );
		bw.newLine();
		bw.write( id1 + ", " + id4 );
		bw.newLine();
		bw.write( id2 + ", " + id3 );
		bw.newLine();
		bw.write( id2 + ", " + id4 );
		bw.newLine();
		bw.write( id3 + ", " + id4 );
		bw.newLine();
	    }
	    //  link ~15% of gambles in 'trios'
	    //  how many trios?  3x = 15/100  =>  x = 1/20
	    int nt = ( int ) Math.round( ng/20.0 );
	    for ( int i = 0; i < nt; i++ ) {
		//  get ids for this trio
		String id1 = DF_INT.format( it.next() );
		String id2 = DF_INT.format( it.next() );
		String id3 = DF_INT.format( it.next() );
		//  create links b/w these ids
		bw.write( id1 + ", " + id2 );
		bw.newLine();
		bw.write( id1 + ", " + id3 );
		bw.newLine();
		bw.write( id2 + ", " + id3 );
		bw.newLine();
	    }
	    //  link ~25% of gambles in 'pairs'
	    //  how many pairs?  2x = 25/100  =>  x = 1/8
	    int np = ( int ) Math.round( ng/8.0 );
	    for ( int i = 0; i < np; i++ ) {
		//  get ids for this pair
		String id1 = DF_INT.format( it.next() );
		String id2 = DF_INT.format( it.next() );
		//  create links b/w these ids
		bw.write( id1 + ", " + id2 );
		bw.newLine();
	    }
	    //  ~50% gambles left unlinked
	    bw.close();
	} //try
	catch(Exception e){
	    e.printStackTrace();
	    System.out.println();
	    System.out.println(" during data generation");
	    System.exit(0);
	}
    }
}
