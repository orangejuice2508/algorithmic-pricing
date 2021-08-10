import pandas as pd
import plotly.graph_objects as go
import constants

# Specify treatment to be analyzed
treatment = "SIM-Q-2"

phi_randomized_uniformly = pd.read_csv("data/q_matrix_init/" + treatment + "_randomized_uniformly.csv")
phi_zeros = pd.read_csv("data/q_matrix_init/" + treatment + "_zeros.csv")
phi_randomized_uniformly_melted = pd.melt(phi_randomized_uniformly, id_vars="alpha", var_name="delta", value_name="phi")
phi_zeros_melted = pd.melt(phi_zeros, id_vars="alpha", var_name="delta", value_name="phi")

boxplots = go.Figure(
    layout=go.Layout(
        yaxis=dict(title="Kollusion ϕ"),
        xaxis=dict(title="Art der Initialisierung")
    ))
boxplots.add_trace(
    go.Box(
        y=phi_randomized_uniformly_melted.phi,
        name="Nach Calvano et al. (2020)",
        showlegend=False,
        boxmean=True))
boxplots.add_trace(
    go.Box(
        y=phi_zeros_melted.phi,
        name="Nach Klein (im Druck)",
        showlegend=False,
        boxmean=True))
boxplots.update_layout(constants.layout)
boxplots.show(config=constants.config)
