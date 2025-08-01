package org.example;

public record PreparedValues(
        double   palpha,
        double   pbeta,
        double[][] tube0,
        double[][] tau0,
        double[][] omega0,
        double[][][] mu0,
        double[][] sij0,
        double[][] tau,
        double[][] mu,
        double[]   countrate,
        double[][][] mu_ijk,
        double[][]  det_ijk,
        double[][][] sij,
        double[][]  alleKanten,
        Übergang[][] alleUeberg,
        double[][][] sij_xyz,
        double[][][] tau_ijk,
        double[] konzNorm,
        double   step,
        double   emin,
        double[] ltf
) {}
