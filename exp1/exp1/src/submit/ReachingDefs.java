package submit;

import java.util.*;

import flow.Flow;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;

public class ReachingDefs implements Flow.Analysis {

    public static class DefSet implements Flow.DataflowObject {
        private Set<Integer> set;
        public static Set<Integer> universalSet;
        public DefSet() { set = new TreeSet<Integer>(); }
        public boolean isEmpty(){ return set.isEmpty();}
        public void setToTop() { set = new TreeSet<Integer>(); }
        public void setToBottom() { set = new TreeSet<Integer>(universalSet); }

        public void meetWith(Flow.DataflowObject o)
        {
            DefSet a = (DefSet)o;
            set.addAll(a.set);
        }

        public void copy(Flow.DataflowObject o)
        {
            DefSet a = (DefSet) o;
            set = new TreeSet<Integer>(a.set);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof DefSet)
            {
                DefSet a = (DefSet) o;
                return set.equals(a.set);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return set.hashCode();
        }
        @Override
        public String toString()
        {
            return set.toString();
        }
        public void genVar(Integer v) {set.add(v);}
        public void killVar(Integer v) {set.remove(v);}
    }

    private DefSet[] in, out;
    private DefSet entry, exit;
    static SortedMap<String, Set<Integer>> varDefs =  new TreeMap<String, Set<Integer>>();
    public void preprocess(ControlFlowGraph cfg) {
        System.out.println("Method: "+cfg.getMethod().getName().toString());
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new DefSet[max];
        out = new DefSet[max];
        qit = new QuadIterator(cfg);

        Set<Integer> s = new TreeSet<Integer>();
        DefSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;

;
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                if (!varDefs.containsKey(def.getRegister().toString()))
                    varDefs.put(def.getRegister().toString(), new TreeSet<Integer>());
                varDefs.get(def.getRegister().toString()).add(q.getID());
            }
        }

        entry = new DefSet();
        exit = new DefSet();
        transferfn.val = new DefSet();
        for (int i=0; i<in.length; i++) {
            in[i] = new DefSet();
            out[i] = new DefSet();
        }

//        System.out.println("Initialization completed.");
    }

    public void postprocess(ControlFlowGraph cfg) {
        System.out.println("entry: "+entry.toString());
        for (int i=1; i<in.length; i++) {
            if (in[i].isEmpty() && out[i].isEmpty()) continue;
            System.out.println(i+" in:  "+in[i].toString());
            System.out.println(i+" out: "+out[i].toString());
        }
        System.out.println("exit: "+exit.toString());
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward() { return true; }

    /* Routines for interacting with dataflow values. */

    public Flow.DataflowObject getEntry()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }
    public Flow.DataflowObject getExit()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }
    public Flow.DataflowObject getIn(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }
    public Flow.DataflowObject getOut(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }
    public void setIn(Quad q, Flow.DataflowObject value)
    {
        in[q.getID()].copy(value);
    }
    public void setOut(Quad q, Flow.DataflowObject value)
    {
        out[q.getID()].copy(value);
    }
    public void setEntry(Flow.DataflowObject value)
    {
        entry.copy(value);
    }
    public void setExit(Flow.DataflowObject value)
    {
        exit.copy(value);
    }

    public Flow.DataflowObject newTempVar() { return new DefSet(); }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        transferfn.visitQuad(q);
        out[q.getID()].copy(transferfn.val);
    }

    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        DefSet val;
        @Override
        public void visitQuad(Quad q) {
            for (RegisterOperand def: q.getDefinedRegisters() ) {
                for (Integer s : varDefs.get(def.getRegister().toString())) {
                    val.killVar(s);
                }
            }
            for (RegisterOperand gen : q.getDefinedRegisters()) {
                val.genVar(q.getID());
            }
        }
    }
}
