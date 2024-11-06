import java.io.*;
import java.util.*;
import java.util.function.Function;

class SubgraphComputation {
    private Set<Integer> V = new HashSet<>();
    private Map<Integer, List<Integer>> adj = new HashMap<>();
    private Map<Integer, List<Integer>> incoming = new HashMap<>();
    private Map<Edge, Integer> w = new HashMap<>();

    private static class Edge {
        int u, v;

        public Edge(int u, int v) {
            this.u = u;
            this.v = v;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Edge edge = (Edge) obj;
            return u == edge.u && v == edge.v;
        }

        @Override
        public int hashCode() {
            return Objects.hash(u, v);
        }

        @Override
        public String toString() {
            return "(" + u + ", " + v + ")";
        }
    }

    public void readFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int numVertices = Integer.parseInt(br.readLine().trim());
            int numEdges = Integer.parseInt(br.readLine().trim());
            for (int i = 0; i < numEdges; i++) {
                String[] parts = br.readLine().trim().split(" ");
                int u = Integer.parseInt(parts[0]);
                int v = Integer.parseInt(parts[1]);
                addEdge(u, v);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File " + filename + " not found.");
        } catch (IOException | NumberFormatException e) {
            System.out.println("File is not in the expected format. Each line should contain two integers separated by a space.");
        }
    }

    public void addEdge(int u, int v) {
        V.add(u);
        V.add(v);
        adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        incoming.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        w.put(new Edge(u, v), 1);  
    }

    public Set<Edge> getEdges() {
        Set<Edge> edges = new HashSet<>();
        for (Map.Entry<Integer, List<Integer>> entry : adj.entrySet()) {
            for (Integer v : entry.getValue()) {
                edges.add(new Edge(entry.getKey(), v));
            }
        }
        return edges;
    }

    public SubgraphComputation reverseGraph() {
        SubgraphComputation reversed = new SubgraphComputation();
        reversed.V.addAll(this.V);
        for (Map.Entry<Integer, List<Integer>> entry : adj.entrySet()) {
            int u = entry.getKey();
            for (int v : entry.getValue()) {
                reversed.addEdge(v, u);
            }
        }
        return reversed;
    }

    public Set<Edge> biasedMSA(Set<Integer> V, Set<Edge> E, int root, Map<Edge, Integer> w, Set<Edge> S, int iteration, String phase) {
        Map<Edge, Integer> biasedW = new HashMap<>();
        for (Edge e : E) {
            biasedW.put(e, S.contains(e) ? 0 : 1);
        }
        return msa(V, E, root, biasedW);
    }

    public Set<Edge> msa(Set<Integer> V, Set<Edge> E, int root, Map<Edge, Integer> w) {
        System.out.println("\nStarting MSA computation");

        // Step 1: Remove edges leading to the root
        Set<Edge> E_filtered = new HashSet<>();
        for (Edge e : E) {
            if (e.v != root) {
                E_filtered.add(e);
            }
        }

        // Step 2: Find minimum incoming edge for every vertex
        Map<Integer, Integer> pi = new HashMap<>();
        Map<Integer, Edge> incomingEdges = new HashMap<>();
        for (Edge e : E_filtered) {
            if (!incomingEdges.containsKey(e.v) || w.get(e) < w.get(incomingEdges.get(e.v))) {
                incomingEdges.put(e.v, e);
            }
        }
        for (Integer v : V) {
            if (incomingEdges.containsKey(v)) {
                pi.put(v, incomingEdges.get(v).u);
            }
        }

        // Step 3: Detect cycles
        Set<Integer> visited = new HashSet<>();
        Integer cycleVertex = null;
        for (Integer v : V) {
            if (cycleVertex != null) break;
            Set<Integer> path = new HashSet<>();
            while (!visited.contains(v) && pi.containsKey(v)) {
                if (path.contains(v)) {
                    cycleVertex = v;
                    break;
                }
                path.add(v);
                v = pi.get(v);
            }
            visited.addAll(path);
        }

        if (cycleVertex == null) {
            Set<Edge> result = new HashSet<>();
            for (Map.Entry<Integer, Integer> entry : pi.entrySet()) {
                result.add(new Edge(entry.getValue(), entry.getKey()));
            }
            System.out.println("MSA result: " + result);
            return result;
        }

        // Step 4: Identify all vertices in the cycle
        Set<Integer> C = new HashSet<>();
        int nextV = cycleVertex;
        do {
            C.add(nextV);
            nextV = pi.get(nextV);
        } while (!C.contains(nextV));

        if (C.size() == 1) {
            System.out.println("Detected self-loop cycle.");
            return new HashSet<>();
        }

        // Step 5: Contract the cycle into a single node
        int vC = Collections.max(V) + 1;
        Set<Integer> V_prime = new HashSet<>(V);
        V_prime.removeAll(C);
        V_prime.add(vC);

        Set<Edge> E_prime = new HashSet<>();
        Map<Edge, Integer> w_prime = new HashMap<>();
        Map<Edge, Edge> correspondence = new HashMap<>();

        for (Edge e : E) {
            if (!C.contains(e.u) && C.contains(e.v)) {
                Edge newEdge = new Edge(e.u, vC);
                int reducedWeight = w.get(e) - w.get(new Edge(pi.get(e.v), e.v));
                if (!w_prime.containsKey(newEdge) || w_prime.get(newEdge) > reducedWeight) {
                    w_prime.put(newEdge, reducedWeight);
                    correspondence.put(newEdge, e);
                }
                E_prime.add(newEdge);
            } else if (C.contains(e.u) && !C.contains(e.v)) {
                Edge newEdge = new Edge(vC, e.v);
                if (!w_prime.containsKey(newEdge) || w_prime.get(newEdge) > w.get(e)) {
                    w_prime.put(newEdge, w.get(e));
                    correspondence.put(newEdge, e);
                }
                E_prime.add(newEdge);
            } else if (!C.contains(e.u) && !C.contains(e.v)) {
                E_prime.add(e);
                w_prime.put(e, w.get(e));
                correspondence.put(e, e);
            }
        }

        // Step 6: Recursive call on the contracted graph
        Set<Edge> tree = msa(V_prime, E_prime, root, w_prime);

        // Step 7: Expand the contracted cycle
        Edge cycleEdge = null;
        for (Edge e : tree) {
            if (e.v == vC) {
                cycleEdge = new Edge(pi.get(correspondence.get(e).v), correspondence.get(e).v);
                break;
            }
        }

        Set<Edge> ret = new HashSet<>();
        for (Edge e : tree) {
            ret.add(correspondence.getOrDefault(e, e));
        }

        for (Integer v : C) {
            ret.add(new Edge(pi.get(v), v));
        }
        ret.remove(cycleEdge);

        System.out.println("Final MSA result: " + ret);
        return ret;
    }

    public SubgraphComputation minimalSpanningSubgraph(Set<Edge> S) {
        SubgraphComputation H = new SubgraphComputation();
        H.V = new HashSet<>(this.V);
        H.adj = new HashMap<>(this.adj);
        H.w = new HashMap<>(this.w);

        int m = H.getEdges().size();
        int iterations = (int) Math.ceil(Math.log(m) / Math.log(2)) + 1;
        Random rand = new Random();
        int root = new ArrayList<>(H.V).get(rand.nextInt(H.V.size()));

        for (int i = 0; i < iterations; i++) {
            Set<Edge> Th = biasedMSA(H.V, H.getEdges(), root, H.w, S, i + 1, "forward");
            SubgraphComputation reversedH = H.reverseGraph();
            Set<Edge> reversedTh = new HashSet<>();
            for (Edge e : Th) {
                reversedTh.add(new Edge(e.v, e.u));
            }
            Set<Edge> I_reversed = reversedH.biasedMSA(reversedH.V, reversedH.getEdges(), root, reversedH.w, reversedTh, i + 1, "inverse");
            Set<Edge> I = new HashSet<>();
            for (Edge e : I_reversed) {
                I.add(new Edge(e.v, e.u));
            }
            Set<Edge> A = new HashSet<>(I);
            A.removeAll(Th);

            H.adj.clear();
            for (Edge e : Th) {
                H.adj.computeIfAbsent(e.u, k -> new ArrayList<>()).add(e.v);
            }
            for (Edge e : A) {
                H.adj.computeIfAbsent(e.u, k -> new ArrayList<>()).add(e.v);
            }
            S.addAll(A);
        }

        System.out.println("Number of edges in the resulting minimal spanning subgraph: " + H.getEdges().size());
        return H;
    }

    public static void timeExecution(Runnable func) {
        long startTime = System.currentTimeMillis();
        func.run();
        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + "ms");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SubgraphComputation <graph_file.txt>");
            System.exit(1);
        }

        String filename = args[0];

        Runnable processGraph = () -> {
            SubgraphComputation graph = new SubgraphComputation();
            graph.readFromFile(filename);
            graph.minimalSpanningSubgraph(new HashSet<>());
        };

        timeExecution(processGraph);
    }
}