const errorHandler = (err, req, res, next) => {
  console.error(err);

  // duplicate email
  if (err.code === 11000) {
    return res.status(409).json({
      success: false,
      message: "An account with that email already exists",
    });
  }

  // mongoose validation
  if (err.name === "ValidationError") {
    const message = Object.values(err.errors)
      .map((e) => e.message)
      .join(", ");
    return res.status(400).json({ success: false, message });
  }

  // bad ObjectId
  if (err.name === "CastError") {
    return res.status(400).json({ success: false, message: "Invalid ID" });
  }

  if (err.type === "entity.parse.failed")
    return res.status(400).json({ success: false, message: "Invalid JSON body" });

  // unexpected errors never leak internals; our own errors carry a friendly message and a statusCode
  const status = err.statusCode || 500;
  const message = err.statusCode ? err.message : "Internal server error";
  res.status(status).json({ success: false, message });
};

module.exports = errorHandler;