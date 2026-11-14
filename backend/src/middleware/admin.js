// use after protect: only users with role "admin" pass
const requireAdmin = (req, res, next) => {
  if (req.user && req.user.role === "admin") return next();
  return res.status(403).json({ success: false, message: "Admin access required" });
};

module.exports = requireAdmin;