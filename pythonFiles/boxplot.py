import pandas as pd
import plotly.graph_objects as go
import constants

# Specify treatments to be analyzed
treatments = ["SIM-P-2", "SIM-P-3", "SIM-Q-2", "SIM-Q-3", "SEQ-P-2", "SEQ-P-3", "SEQ-Q-2", "SEQ-Q-3"]
# Specify whether only coordinated runs should be analyzed
condition_on_coordinative_runs = False

boxplots = go.Figure(
    layout=go.Layout(
        yaxis=dict(title="Kollusion ϕ"),
        xaxis=dict(title="Treatment")
    ))

for treatment in treatments:
    treatment_phi = pd.read_csv("data/heatmap_alpha_delta/unsmoothed/" + treatment + "_phi.csv")
    treatment_phi_melted = pd.melt(treatment_phi, id_vars="alpha", var_name="delta", value_name="phi")
    treatment_percentage = pd.read_csv("data/heatmap_alpha_delta/unsmoothed/" + treatment + "_percentage.csv")
    treatment_percentage_melted = pd.melt(treatment_percentage, id_vars="alpha", var_name="delta",
                                          value_name="percentage")
    treatment_data = treatment_phi_melted
    treatment_data["percentage"] = treatment_percentage_melted.percentage

    if condition_on_coordinative_runs:
        treatment_data = treatment_data[treatment_data["percentage"] > 0]

    treatment = treatment[0:7]

    boxplots.add_trace(
        go.Box(
            y=treatment_data.phi,
            name=treatment,
            showlegend=False,
            boxmean=True))

boxplots.update_layout(constants.layout)
boxplots.show(config=constants.config)
