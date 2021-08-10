import time

import numpy as np
import pandas as pd
import plotly.colors
import plotly.graph_objects as go
import plotly.express as px
import constants
import statsmodels.api as sm

# Specify treatments to be analyzed (Order according to highest degree of tacit collusion)
treatments = ["SIM-P", "SIM-Q", "SEQ-P", "SEQ-Q"]

colors = plotly.colors.DEFAULT_PLOTLY_COLORS
i = 0

# Plot with degree of tacit collusion
for treatment in treatments:
    fig = go.Figure(layout=go.Layout(
        xaxis=dict(title="Anzahl der Firmen"), yaxis=dict(title="Kollusion ϕ"), title=treatment))
    df = pd.read_csv("data/market_size/" + treatment + ".csv")

    # Line with degree of tacit collusion
    fig.add_trace(go.Scatter(
        x=df.get("MARKET SIZE"), y=df.get("DEGREE OF TACIT COLLUSION"),
        mode="lines",
        line=dict(color=colors[i], shape="spline", width=3)
    ))

    # OLS trendline
    trendline = px.scatter(df, x="MARKET SIZE", y="DEGREE OF TACIT COLLUSION", trendline="ols",
                           color_discrete_sequence=[colors[i]])
    trendline_data = trendline.data[1]
    fig.add_trace(trendline_data)

    # Vertical line at y=0
    y = np.empty(df.get("MARKET SIZE").size); y.fill(0)
    fig.add_trace(go.Scatter(
        x=df.get("MARKET SIZE"), y=y,
        mode="lines",
        line=dict(color='black', dash="longdash")
    ))

    # Vertical line at y=1
    y.fill(1)
    fig.add_trace(go.Scatter(
        x=df.get("MARKET SIZE"), y=y,
        mode="lines",
        line=dict(color='black', dash="longdash")
    ))

    fig.update_layout(constants.layout, showlegend=False)
    fig.show(config=constants.config)
    i += 1
    time.sleep(3)

# Regression for tests
for treatment in treatments:
    df = pd.read_csv("data/market_size/" + treatment + ".csv")

    x = df.get("MARKET SIZE")
    y = df.get("DEGREE OF TACIT COLLUSION")

    x = sm.add_constant(x)
    result = sm.OLS(y, x).fit()
    print(treatment)
    print(result.summary())

# Print (conditional) mean degree of tacit collusion and percentage of collusion
for treatment in treatments:
    df = pd.read_csv("data/market_size/" + treatment + ".csv")

    print("\n" + treatment)
    print("MEAN DEGREE OF TACIT COLLUSION: " + str(np.mean(df.get("DEGREE OF TACIT COLLUSION"))))
    print("MEAN PERCENTAGE OF COORDINATION: " + str(np.mean(df.get("PERCENTAGE OF COORDINATION"))))

    coordinative_runs = df[df["PERCENTAGE OF COORDINATION"] > 0]
    print("CONDTIONIONAL MEAN DEGREE OF TACIT COLLUSION: " + str(np.mean(coordinative_runs.get("DEGREE OF TACIT COLLUSION"))))
