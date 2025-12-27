import { NavLink as RouterNavLink, NavLinkProps } from "react-router-dom";
import { forwardRef, ReactNode } from "react";
import { cn } from "@/lib/utils";
import { Loader2 } from "lucide-react";

interface NavLinkPropsExtended
  extends Omit<NavLinkProps, "className"> {
  className?: string;
  activeClassName?: string;
  pendingClassName?: string;
  /** Optional icon on the left */
  icon?: ReactNode;
  /** Optional icon on the right (e.g., chevron) */
  endIcon?: ReactNode;
  /** Show loading spinner when pending */
  showPendingIndicator?: boolean;
  /** Children (text or custom content) */
  children: ReactNode;
}

const NavLink = forwardRef<HTMLAnchorElement, NavLinkPropsExtended>(
  (
    {
      className = "",
      activeClassName = "",
      pendingClassName = "",
      icon,
      endIcon,
      showPendingIndicator = true,
      children,
      to,
      ...props
    },
    ref
  ) => {
    return (
      <RouterNavLink
        ref={ref}
        to={to}
        className={({ isActive, isPending }) =>
          cn(
            // Base styles — clean, modern, accessible
            "flex items-center gap-3 px-4 py-3 rounded-lg font-medium text-sm transition-all duration-200",
            "hover:bg-accent/80 hover:text-accent-foreground",
            "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
            "disabled:pointer-events-none disabled:opacity-50",

            // Conditional states
            isActive &&
              cn(
                "bg-primary/10 text-primary font-semibold shadow-sm",
                activeClassName
              ),
            isPending &&
              cn(
                "text-muted-foreground bg-accent/50",
                pendingClassName
              ),

            // User-provided override
            className
          )
        }
        {...props}
      >
        {({ isPending }) => (
          <>
            {/* Left Icon */}
            {icon && <span className="text-lg">{icon}</span>}

            {/* Main Content */}
            <span className="flex-1 truncate">{children}</span>

            {/* Pending Indicator */}
            {isPending && showPendingIndicator && (
              <Loader2 className="w-4 h-4 animate-spin text-primary" />
            )}

            {/* Right Icon */}
            {endIcon && !isPending && (
              <span className="text-muted-foreground">{endIcon}</span>
            )}
          </>
        )}
      </RouterNavLink>
    );
  }
);


NavLink.displayName = "NavLink";

export { NavLink };