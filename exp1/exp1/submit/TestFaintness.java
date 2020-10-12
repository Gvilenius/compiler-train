package submit;

class TestFaintness {
    /**
     * In this method all variables are faint because the final value is never used.
     * Sample out is at src/test/Faintness.out
     */
    void test1() {
        int x = 2;
        int y = x + 2;
        int z = x + y;
        return;
    }
//
//    /**
//     * Write your test cases here. Create as many methods as you want.
//     * Run the test from root dir using
//     * ./run.sh flow.Flow submit.MySolver submit.Faintness submit.TestFaintness
//     */
    int test2(int u) {
        // faint: x,y,z. x is unlive and y,z are faint after the quad
      int x = u;
      // faint: u,y,z.
      int y = x + 2;
      // faint: u,y,z.  z is unlive so y is faint.
      int z = x + y;
      // faint: u,y,z. They are unlive.
      return x;
    }
    int test3(int x, int y) {
        // faint: u, z, they are unlive
        int z = x+y;
        // faint: u, x, y, they are unlive. z is used to calculate -z.
        int u = -z;
        // faint: x, y, z, they are unlive
        return u;
    }
//    BB2	(in: BB0 (ENTRY), out: BB1 (EXIT))
//1   ADD_I                   T3 int,	R1 int,	R2 int
//2   MOVE_I                  R4 int,	T3 int
//3   NEG_I                   T3 int,	R4 int
//4   MOVE_I                  R5 int,	T3 int
//5   RETURN_I                R5 int
    void test4(int a,int b) {
        //faint: c
        int c[] = new int[a];
        //faint: c. a is used to calculate a-1 which is live, b is used to store so it's also live
        c[a-1] = b;
        //faint: c. It is unlive while a,b are live
        test3(a, b);
        //faint: a,b,c. They are unlive.
    }
//BB2	(in: BB0 (ENTRY), out: BB1 (EXIT))
//1   NEWARRAY                T3 int[],	R1 int,	int[]
//2   MOVE_A                  R4 int[],	T3 int[]
//3   SUB_I                   T5 int,	R1 int,	IConst: 1
//4   NULL_CHECK              T-1 <g>,	R4 int[]
//5   BOUNDS_CHECK            R4 int[],	T5 int,	T-1 <g>
//6   ASTORE_I                R2 int,	R4 int[],	T5 int,	T-1 <g>
//8   NULL_CHECK              T-1 <g>,	R0 TestFaintness
//7   INVOKEVIRTUAL_I%        T7 int,	submit.TestFaintness.test3 (II)I,	(R0 TestFaintness, R1 int, R2 int)
//9   RETURN_V
}
