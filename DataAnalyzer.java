
import java.io.*;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.*;



public class DataAnalyzer {


    static final boolean DEBUG = false;


    static final int HI = 0;
    static final int MD = 1;
    static final int LO = 2;

    static final String[] LVLS = new String[] { "HI", "MD", "LO" };

    static final Random RND = new Random();

    static final DecimalFormat DF_INT  = new DecimalFormat( "000"    );
    static final DecimalFormat DF_RET  = new DecimalFormat( "00.00" );
    static final DecimalFormat DF_PROB = new DecimalFormat( "0.00"  );


    int nGambles;
    Gamble[] gambles;
    int attrFav1, attrFav2;
    ArrayList gf1, gf2;



    DataAnalyzer( String s ) {
        readData(s);
        attrFav1 = 15;
        attrFav2 = 13;
        findFavGambles();
        out( "\n\n\ngambles w attr " + attrFav1 + ":\n" );
        analyzeLinks(gf1);
        out( "\n\n\ngambles w attr " + attrFav2 + ":\n" );
        analyzeLinks(gf2);
    }


    void analyzeLinks( ArrayList al ) {
        out( "# gambles = " + al.size() + "\n" );
        HashSet hs = new HashSet();
        for ( int i = 0; i < al.size(); i++ ) {
            Gamble g = ( Gamble ) al.get(i);
            hs.add( g.links );
        }
        out( "# linked groups = " + hs.size() + "\n" );
        for ( Iterator it = hs.iterator(); it.hasNext(); ) {
            Gamble[] gg = ( Gamble[] ) it.next();
            out( "  group w/ " + gg.length + " link(s)  " );
            List list = Arrays.asList( gg );
            HashSet hs2 = new HashSet( list );
            out( "  # links in group = " + hs2.size() + "\n" );
            for ( Iterator it2 = hs2.iterator(); it2.hasNext(); ) {
                Gamble g2 = ( Gamble ) it2.next();
                out( "    " + g2 + " " );
                int count = 0;
                for ( int j = 0; j < gg.length; j++ )
                    if ( gg[j] == g2 )
                        count++;
                out( " occurs " + count + " time(s) " );
                if ( g2.attr == attrFav1 || g2.attr == attrFav2 ) {
                    double[] ps = new double[3];
                    ps[HI] = g2.probs[HI];
                    ps[MD] = g2.probs[MD];
                    ps[LO] = g2.probs[LO];
                    ps[HI] += ( ps[LO] /= 2 );
                    double expRet = ps[HI]*g2.rets[HI] +
                                    ps[MD]*g2.rets[MD] +
                                    ps[LO]*g2.rets[LO];
                    out( " expRet = " + DF_RET.format( expRet ) + "\n" );
                } else {
                    out( "\n" );
                }
            }
        }
    }


    void findFavGambles() {
        gf1 = new ArrayList();
        gf2 = new ArrayList();
        for ( int i = 0; i < nGambles; i++ ) {
            if ( gambles[i].attr == attrFav1 )
                gf1.add( gambles[i] );
            if ( gambles[i].attr == attrFav2 )
                gf2.add( gambles[i] );
        }
    }

    void readData( String s ) {
        out( "readData( " + s + " )\n" );
        try {
            BufferedReader br = new BufferedReader( new FileReader(s) );
            ArrayList[] als = new ArrayList[3];
            for ( int i = 0; i < 3; i++ )
                als[i] = new ArrayList();
            int section = -1;
            String line = null;
            while ( ( line = br.readLine() ) != null ) {
                line = line.trim();
                if ( line.length() == 0 )
                    continue;
                if ( line.charAt(0) == '#' ) {
                    //  new section of data file
                    if ( line.indexOf( "gamble(" ) != -1 )
                        section = 0;
                    else if ( line.indexOf( "gambleatts(" ) != -1 )
                        section = 1;
                    else if ( line.indexOf( "link(" ) != -1 )
                        section = 2;
                    else
                        throw new RuntimeException(
                                "unexpected line in data file: " + line );
                    continue;
                }
                //  save all tokens as doubles
                String[] ss = line.split( "[, ]+" );
                double[] dd = new double[ss.length];
                for ( int i = 0; i < ss.length; i++ )
                    dd[i] = Double.parseDouble( ss[i] );
                als[section].add( dd );
            }
            debug( "read " + als[0].size() + " gambles, " +
                    als[1].size() + " attrs, " +
                    als[2].size() + " links\n" );
            if ( als[0].size() != als[1].size() )
                throw new RuntimeException(
                        "# gambles != # attrs" );
            //  build gambles
            nGambles = als[0].size();
            gambles = new Gamble[nGambles];
            for ( int i = 0; i < nGambles; i++ ) {
                double[] ddg = ( double[] ) als[0].get(i);
                double[] dda = ( double[] ) als[1].get(i);
                if ( ddg[0] != i || dda[0] != i )
                    throw new RuntimeException(
                            "gambles or attrs not in order" );
                if ( ddg.length != 7 || dda.length != 5 )
                    throw new RuntimeException(
                            "wrong # of tokens in gamble or attr " + i );
                //  rets & probs
                double[] rets  = new double[] { ddg[1], ddg[3], ddg[5] };
                double[] probs = new double[] { ddg[2], ddg[4], ddg[6] };
                //  normalize probs from file
                normalize( probs );
                //  map attr string to int
                int attr = 0;
                for ( int j = 1; j < 5; j++ )
                    if ( dda[j] == 0 )
                        ;
                    else if ( dda[j] == 1 )
                        attr += 1 << (4-j);
                    else
                        throw new RuntimeException(
                                "non-binary attr for " + i );
                //  create gamble
                gambles[i] = new Gamble( i, probs, rets, attr );
            }
            //  link gambles
            for ( int i = 0; i < als[2].size(); i++ ) {
                double[] dd = ( double[] ) als[2].get(i);
                Gamble[] links1 = gambles[ ( int ) dd[0] ].links;
                Gamble[] links2 = gambles[ ( int ) dd[1] ].links;
                if ( links1 != links2 ) {
                    Gamble[] links3 = ( Gamble[] ) merge( links1, links2 );
                    for ( int j = 0; j < links1.length; j++ )
                        links1[j].links = links3;
                    for ( int j = 0; j < links2.length; j++ )
                        links2[j].links = links3;
                }
            }
        } catch ( Exception e ) {
            out( "exception reading returns and links: " + e + "\n" );
            e.printStackTrace();
            out( "exiting\n" );
            System.exit(1);
        }
    }


    class Gamble {

        final int id;
        final double[] probs;
        final double[] rets;
        final int attr;
        final String s;
        Gamble[] links = new Gamble[] { this };
        Outcome[] outcomes = new Outcome[0];

        Gamble( int id, double[] probs, double[] rets, int attr ) {
            this.id = id;
            this.probs = probs;
            this.rets = rets;
            this.attr = attr;
            s = "id=" + DF_INT.format(id) +
                    " p={ " + arrayStr( probs, DF_PROB ) + "}" +
                    " r={ " + arrayStr( rets, DF_RET ) + "}" +
                    " a=" + ( ( attr < 10 ) ? " " : "" ) + attr;
        }

        public String toString() { return s; }
    }


    class Outcome {

        final int idx;
        final int attrSway;
        final int[] lnkLvls;
        final double[] probs;
        final double rnd;
        final int lvl;
        final double pyf;
        final String s;

        Outcome( int idx, int attrSway, int[] lnkLvls,
                 double[] probs, double rnd, int lvl, double pyf ) {
            this.idx = idx;
            this.attrSway = attrSway;
            this.lnkLvls = lnkLvls;
            this.probs = probs;
            this.rnd = rnd;
            this.lvl = lvl;
            this.pyf = pyf;
            s = "@=" + DF_INT.format(idx) +
                    " a=" + attrSway +
                    " l=" + lnkLvls[HI] + lnkLvls[MD] + lnkLvls[LO] +
                    " p={ " + arrayStr( probs, DF_PROB ) + "}" +
                    " r=" + DF_PROB.format(rnd) +
                    " l=" + LVLS[lvl] +
                    " p=" + DF_RET.format(pyf);
        }

        public String toString() { return s; }
    }


    static Object merge( Object[] in1, Object[] in2 ) {
        Class c = in1[0].getClass();
        int len = in1.length + in2.length;
        Object[] out = ( Object[] ) Array.newInstance(c,len);
        int count = 0;
        for ( int i = 0; i < in1.length; i++ )
            out[count++] = in1[i];
        for ( int i = 0; i < in2.length; i++ )
            out[count++] = in2[i];
        return out;
    }


    static void normalize( double[] d ) {
        double total = 0;
        for ( int i = 0; i < d.length; i++ )
            total += d[i];
        for ( int i = 0; i < d.length; i++ )
            d[i] /= total;
    }


    static String arrayStr( double[] dd, DecimalFormat df ) {
        StringBuffer sb = new StringBuffer(
                dd.length*(df.toPattern().length()+1) );
        for ( int i = 0; i < dd.length; i++ )
            sb.append( df.format( dd[i] ) ).append( ' ' );
        return sb.toString();
    }


    static void debug( String s ) { if ( DEBUG ) out(s); }
    static void out( String s ) {
        System.out.print(s);
        System.out.flush();
    }


    public static void main( String[] args ) {
        new DataAnalyzer( args[0] );
    }
}
