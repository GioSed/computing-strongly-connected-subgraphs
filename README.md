# minimal-scss-computation

This repository contains an implementation of an algorithm to compute a **Minimal Strongly Connected Spanning Subgraph (SCSS)** from a given strongly connected graph. The goal of the algorithm is to identify a subgraph with the least number of edges that still maintains strong connectivity.

## Table of Contents
- [Overview](#overview)
- [Algorithm](#algorithm)
- [Installation](#installation)
- [Usage](#usage)

## Overview
Given a strongly connected graph, this algorithm iteratively computes a minimal strongly connected spanning subgraph (SCSS) by removing redundant edges while ensuring that the graph remains strongly connected. The result is a subgraph with the minimum number of edges that still allows every node to be reachable from any other node via directed paths.

This implementation is part of my thesis work, which focuses on efficient SCSS computation, leveraging an optimized version of **Edmonds' Minimum Spanning Arborescence (MSA)** algorithm, modified with biased edge weights.

## Algorithm
The core of this algorithm involves:
1. **Iterative Branch Computation**: The algorithm computes branches of the strongly connected subgraph iteratively, identifying and retaining only the edges essential for strong connectivity.
2. **Edmonds' Biased MSA Algorithm**: Each branch is computed using Edmonds' MSA algorithm with biased weights:
   - **Zero-Weighted Edges**: Edges prioritized for inclusion.
   - **One-Weighted Edges**: Secondary edges that are included only when needed to maintain connectivity.

### Steps:
1. **Graph Input**: The algorithm starts with a strongly connected directed graph.
2. **Branch Computation**: Biased MSA is applied iteratively to find minimal branches that connect nodes.
3. **Cycle Contraction**: Cycles are detected and contracted to simplify the graph while preserving connectivity.
4. **Expansion**: Contracted cycles are expanded iteratively to form the final SCSS.
5. **Output**: The output is a minimal SCSS with the least number of edges needed for strong connectivity.

## Installation
To use this repository, clone it and compile the Java program.

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/minimal-scs-computation.git
   cd minimal-scs-computation
   ```

2. Compile the Java program:
   ```bash
   javac SubgraphComputation.java
   ```

3. Run the program with a graph file:
   ```bash
   java SubgraphComputation <path_to_graph_file.txt>
   ```

   - The input file should contain the graph in the following format:
     - First line: Number of vertices.
     - Second line: Number of edges.
     - Subsequent lines: Each line represents an edge with two integers, `u` and `v`, indicating a directed edge from node `u` to node `v`.

## Usage
To run the algorithm from the command line, use the following command:

```bash
java SubgraphComputation <graph_file.txt>
```

### Example
For a graph file `graph.txt` with the following contents:
```
5
6
1 2
2 3
3 4
4 5
5 1
2 4
```

You would run the program as follows:
```bash
java SubgraphComputation graph.txt
```

The output will include details about each iteration, the minimal spanning subgraph result, and performance statistics.

