import time

import pandas as pd
import plotly.graph_objects as go
import constants

# Specify treatments to be analyzed
treatments = ["SIM-P-2", "SIM-P-3", "SIM-Q-2", "SIM-Q-3", "SEQ-P-2", "SEQ-P-3", "SEQ-Q-2", "SEQ-Q-3"]
# Specify type of smoothing: either "plotly" for automated smoothing or "moore" for Moore Neighborhood smoothing
smooth_type = "moore"

file_ending_phi = "_phi.csv"
file_ending_percentage = "_percentage.csv"
base_path, zsmooth, contours_size, heatmap_colorscale, contours_colorscale = None, None, None, None, None

if smooth_type == "plotly":
    base_path = "data/heatmap_alpha_delta/unsmoothed/"
    zsmooth = "best"
    contours_size = 25
    heatmap_colorscale = "hsv"
    contours_colorscale = "greys"
elif smooth_type == "moore":
    base_path = "data/heatmap_alpha_delta/smoothed/"
    zsmooth = False
    contours_size = 20
    heatmap_colorscale = constants.colorscale_heatmap_r
    contours_colorscale = constants.colorscale_all_black

for treatment in treatments:
    treatment_phi = pd.read_csv(base_path + treatment + file_ending_phi)
    treatment_phi_melted = pd.melt(treatment_phi, id_vars="alpha", var_name="delta", value_name="phi")
    heatmap = go.Figure(
        data=go.Heatmap(
            x=treatment_phi_melted.alpha,
            y=treatment_phi_melted.delta,
            z=treatment_phi_melted.phi,
            zmin=0, zmid=1, zmax=2,
            colorscale=heatmap_colorscale,
            colorbar=dict(title="Kollusion ϕ"),
            zsmooth=zsmooth),
        layout=go.Layout(
            xaxis=dict(title="Lernfaktor α"),
            yaxis=dict(title="Diskontierungsfaktor δ")
        )
    )
    treatment_percentage = pd.read_csv(base_path + treatment + file_ending_percentage)
    treatment_percentage_melted = pd.melt(treatment_percentage, id_vars="alpha", var_name="delta",
                                          value_name="percentage")
    contours = go.Figure(go.Contour(
        x=treatment_percentage_melted.alpha,
        y=treatment_percentage_melted.delta,
        z=treatment_percentage_melted.percentage,
        showscale=False,
        colorscale=contours_colorscale,
        line=dict(width=1.5),
        contours=dict(
            coloring='lines',
            showlabels=True,
            start=0, end=100, size=contours_size,
            labelfont=dict(size=20)
        )
    ))
    heatmap.add_trace(contours.data[0])
    heatmap.update_layout(constants.layout)
    heatmap.update_layout(yaxis=dict(tickvals=[0.80, 0.85, 0.90, 0.95, 0.99]))
    heatmap.show(config=constants.config)
    time.sleep(3)