import pandas as pd
import plotly.graph_objects as go
import constants

# Specify treatments to be analyzed (Order according to highest degree of tacit collusion)
treatments = ["SIM-P-3", "SIM-Q-3", "SEQ-P-3", "SEQ-Q-3"]

# Plot with degree of tacit collusion
fig = go.Figure(layout=go.Layout(
    xaxis=dict(title="Gewichtungsfaktor γ"), yaxis=dict(title="Kollusion ϕ")))
for treatment in treatments:
    df = pd.read_csv("data/analyze_gamma/" + treatment + ".csv")

    fig.add_trace(go.Scatter(
        x=df.get("Gamma"), y=df.get("Phi"),
        mode="lines",
        name=treatment,
        line=dict(shape="spline")
    ))
fig.update_layout(constants.layout)
fig.show(config=constants.config)

# Plot with percentage of coordination
fig = go.Figure(layout=go.Layout(xaxis=dict(title="Gewichtungsfaktor γ"), yaxis=dict(title="Koordination ψ (in %)")))
for treatment in treatments:
    df = pd.read_csv("data/analyze_gamma/" + treatment + ".csv")

    fig.add_trace(go.Scatter(
        x=df.get("Gamma"), y=df.get("Percentage"),
        mode="lines",
        name=treatment,
        line=dict(shape="spline")
    ))
fig.update_layout(constants.layout)
fig.show(config=constants.config)

# Plot with degree of tacit collusion conditioned on coordinative runs
fig = go.Figure(layout=go.Layout(
    xaxis=dict(title="Gewichtungsfaktor γ"), yaxis=dict(title="Kollusion ϕ (bedingt)")))
for treatment in treatments:
    df = pd.read_csv("data/analyze_gamma/" + treatment + ".csv")

    fig.add_trace(go.Scatter(
        x=df.get("Gamma"), y=df.get("Phi_bedingt"),
        mode="lines",
        name=treatment,
        line=dict(shape="spline")
    ))
fig.update_layout(constants.layout)
fig.show(config=constants.config)
