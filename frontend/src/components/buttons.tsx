import type { ReactNode, ButtonHTMLAttributes, AnchorHTMLAttributes } from 'react';
import '../styles/buttons-chips.css';

export type ButtonVariant = 'primary' | 'filter' | 'secondary' | 'tertiary';

interface BaseButtonProps {
  children: ReactNode;
  variant?: ButtonVariant;
  className?: string;
}

type ButtonAsButtonProps = BaseButtonProps &
  ButtonHTMLAttributes<HTMLButtonElement> & { href?: undefined };

type ButtonAsAnchorProps = BaseButtonProps &
  AnchorHTMLAttributes<HTMLAnchorElement> & { href: string };

export type ButtonProps = ButtonAsButtonProps | ButtonAsAnchorProps;

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  className = '',
  href,
  ...props
}) => {
  const variantClass = variant ? `btn-${variant}` : '';
  const combinedClasses = `btn ${variantClass} ${className}`.trim();

  if (href) {
    return (
      <a href={href} className={combinedClasses} {...(props as AnchorHTMLAttributes<HTMLAnchorElement>)}>
        {children}
      </a>
    );
  }

  return (
    <button className={combinedClasses} {...(props as ButtonHTMLAttributes<HTMLButtonElement>)}>
      {children}
    </button>
  );
};
