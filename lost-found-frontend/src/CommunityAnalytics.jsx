
import React, { useMemo, useState } from "react";
import "./CommunityAnalytics.css";

export default function CommunityAnalytics({
  lostItems = [],
  foundItems = [],
  allItems = [],
  onRefresh,
}) {
  const [refreshing, setRefreshing] = useState(false);

  const analytics = useMemo(() => {
    const categoryCounts = {};

    allItems.forEach((item) => {
      const category =
        item?._categoryName ||
        item?.category?.name ||
        item?.categoryName ||
        "Other";

      categoryCounts[category] = (categoryCounts[category] || 0) + 1;
    });

    const categories = Object.entries(categoryCounts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);

    const total = allItems.length;
    const lost = lostItems.length;
    const found = foundItems.length;

    return {
      total,
      lost,
      found,
      categories: categories.slice(0, 6),
      busiestCategory: categories[0]?.name || "No reports yet",
    };
  }, [allItems, lostItems, foundItems]);

  const maxCount = Math.max(
    1,
    ...analytics.categories.map((category) => category.count)
  );

  async function handleRefresh() {
    if (!onRefresh || refreshing) return;

    setRefreshing(true);

    try {
      await onRefresh();
    } catch (error) {
      console.error("Unable to refresh community analytics:", error);
    } finally {
      setRefreshing(false);
    }
  }

  return (
    <section
      className="community-analytics"
      id="community-analytics"
      aria-labelledby="community-analytics-title"
    >
      <div className="ca-header">
        <div className="ca-heading">
          <span className="ca-eyebrow">COMMUNITY INSIGHTS</span>
          <h2 id="community-analytics-title">
            Community Analytics
          </h2>
          <p>
            See how our community is working together to find
            lost belongings.
          </p>
        </div>

        <button
          type="button"
          className="ca-refresh"
          onClick={handleRefresh}
          disabled={!onRefresh || refreshing}
        >
          <span aria-hidden="true">
            {refreshing ? "⏳" : "↻"}
          </span>
          {refreshing ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      <div className="ca-stats">
        <article className="ca-stat-card">
          <div className="ca-stat-icon" aria-hidden="true">
            📋
          </div>
          <p className="ca-stat-label">Total Reports</p>
          <p className="ca-stat-value">{analytics.total}</p>
          <p className="ca-stat-description">
            All available lost and found reports
          </p>
        </article>

        <article className="ca-stat-card ca-lost-card">
          <div className="ca-stat-icon" aria-hidden="true">
            🔎
          </div>
          <p className="ca-stat-label">Lost Items</p>
          <p className="ca-stat-value">{analytics.lost}</p>
          <p className="ca-stat-description">
            Items reported as lost
          </p>
        </article>

        <article className="ca-stat-card ca-found-card">
          <div className="ca-stat-icon" aria-hidden="true">
            🤝
          </div>
          <p className="ca-stat-label">Found Items</p>
          <p className="ca-stat-value">{analytics.found}</p>
          <p className="ca-stat-description">
            Items reported as found
          </p>
        </article>
      </div>

      <div className="ca-breakdown">
        <div className="ca-breakdown-header">
          <div>
            <h3>Reports by Category</h3>
            <p>Categories with the most community reports</p>
          </div>

          <span className="ca-category-count">
            {analytics.categories.length} categories
          </span>
        </div>

        {analytics.categories.length > 0 ? (
          <div className="ca-chart" role="list">
            {analytics.categories.map((category) => {
              const percentage =
                (category.count / maxCount) * 100;

              return (
                <div
                  className="ca-chart-row"
                  key={category.name}
                  role="listitem"
                >
                  <div className="ca-chart-label">
                    <span>{category.name}</span>
                    <strong>{category.count}</strong>
                  </div>

                  <div
                    className="ca-bar-track"
                    role="progressbar"
                    aria-label={`${category.name} reports`}
                    aria-valuemin={0}
                    aria-valuemax={maxCount}
                    aria-valuenow={category.count}
                  >
                    <div
                      className="ca-bar-fill"
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="ca-empty">
            <span aria-hidden="true">📊</span>
            <h4>No report data yet</h4>
            <p>
              Category statistics will appear when reports
              become available.
            </p>
          </div>
        )}
      </div>

      <div className="ca-highlight">
        <span className="ca-highlight-icon" aria-hidden="true">
          🏆
        </span>
        <div>
          <p className="ca-highlight-label">
            MOST REPORTED CATEGORY
          </p>
          <h3>{analytics.busiestCategory}</h3>
        </div>
      </div>

      <p className="ca-footnote">
        Statistics are calculated from the reports currently
        loaded in HERO FIND. They do not represent confirmed
        item recoveries.
      </p>
    </section>
  );
}
